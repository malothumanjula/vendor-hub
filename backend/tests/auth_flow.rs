use axum::{
    extract::State,
    http::{HeaderMap, HeaderValue, StatusCode},
    Json,
};
use serde_json::json;

#[tokio::test]
async fn register_login_current_user_and_auth_failures_work() {
    let pool = sqlx::SqlitePool::connect("sqlite::memory:").await.unwrap();
    let app_state = street_vendor_backend::AppState::new_for_tests(&pool, "test-secret").await;

    let register = street_vendor_backend::routes::auth::register(
        State(app_state.clone()),
        Json(json!({
            "email": "demo@example.com",
            "password": "StrongPass123!",
            "name": "Demo User",
            "role": "CUSTOMER"
        })),
    )
    .await;

    assert!(register.is_ok(), "registration should succeed");
    let body = register.unwrap().0;
    assert_eq!(body["user"]["email"], "demo@example.com");
    let token = body["token"].as_str().unwrap().to_string();

    let duplicate = street_vendor_backend::routes::auth::register(
        State(app_state.clone()),
        Json(json!({"email":"demo@example.com","password":"StrongPass123!","name":"Demo User","role":"CUSTOMER"})),
    ).await;
    assert_eq!(duplicate.unwrap_err(), StatusCode::CONFLICT);

    let login = street_vendor_backend::routes::auth::login(
        State(app_state.clone()),
        Json(json!({
            "email": "demo@example.com",
            "password": "StrongPass123!"
        })),
    )
    .await;

    assert!(login.is_ok(), "login should succeed");
    let payload = login.unwrap().0;
    assert_eq!(payload["user"]["email"], "demo@example.com");
    assert!(payload["token"].as_str().is_some());

    let invalid_password = street_vendor_backend::routes::auth::login(
        State(app_state.clone()),
        Json(json!({"email":"demo@example.com","password":"WrongPass123!"})),
    )
    .await;
    assert_eq!(invalid_password.unwrap_err(), StatusCode::UNAUTHORIZED);

    let missing_token =
        street_vendor_backend::routes::users::me(State(app_state.clone()), HeaderMap::new()).await;
    assert_eq!(missing_token.unwrap_err(), StatusCode::UNAUTHORIZED);

    let mut headers = HeaderMap::new();
    headers.insert(
        "authorization",
        HeaderValue::from_str(&format!("Bearer {token}")).unwrap(),
    );
    let me = street_vendor_backend::routes::users::me(State(app_state.clone()), headers)
        .await
        .unwrap()
        .0;
    assert_eq!(me["email"], "demo@example.com");

    let mut invalid_headers = HeaderMap::new();
    invalid_headers.insert(
        "authorization",
        HeaderValue::from_static("Bearer invalid-token"),
    );
    let invalid_token =
        street_vendor_backend::routes::users::me(State(app_state), invalid_headers).await;
    assert_eq!(invalid_token.unwrap_err(), StatusCode::UNAUTHORIZED);
}
