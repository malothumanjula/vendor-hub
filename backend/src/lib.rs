pub mod config;
pub mod middleware;
pub mod models;
pub mod repositories;
pub mod routes;
pub mod services;

use std::fs;

use axum::{
    http::{HeaderMap, StatusCode},
    routing::{get, patch, post},
    Router,
};
use jsonwebtoken::{decode, Algorithm, DecodingKey, Validation};
use serde::Deserialize;
use sqlx::{sqlite::SqlitePoolOptions, SqlitePool};

#[derive(Clone)]
pub struct AppState {
    pub pool: SqlitePool,
    pub jwt_secret: String,
}

#[derive(Debug, Deserialize)]
struct AuthClaims {
    sub: String,
    role: String,
    exp: usize,
}

pub async fn authenticated_user(
    headers: &HeaderMap,
    state: &AppState,
) -> Result<models::AuthenticatedUser, StatusCode> {
    let token = headers
        .get("authorization")
        .and_then(|value| value.to_str().ok())
        .and_then(|value| value.strip_prefix("Bearer "))
        .ok_or(StatusCode::UNAUTHORIZED)?;
    let claims = decode::<AuthClaims>(
        token,
        &DecodingKey::from_secret(state.jwt_secret.as_bytes()),
        &Validation::new(Algorithm::HS256),
    )
    .map_err(|_| StatusCode::UNAUTHORIZED)?
    .claims;
    let _ = claims.exp;
    Ok(models::AuthenticatedUser {
        id: claims.sub,
        role: claims.role,
    })
}

impl AppState {
    pub async fn new(database_url: &str, jwt_secret: &str) -> Result<Self, sqlx::Error> {
        if let Some(stripped) = database_url.strip_prefix("sqlite://") {
            let path = std::path::Path::new(stripped);
            if let Some(parent) = path.parent() {
                if !parent.as_os_str().is_empty() {
                    fs::create_dir_all(parent).map_err(|error| sqlx::Error::Io(error.into()))?;
                }
            }
        }

        let pool = SqlitePoolOptions::new()
            .max_connections(10)
            .connect(database_url)
            .await?;

        let state = Self {
            pool,
            jwt_secret: jwt_secret.to_string(),
        };

        state.run_migrations().await?;
        Ok(state)
    }

    pub async fn new_for_tests(pool: &SqlitePool, jwt_secret: &str) -> Self {
        let state = Self {
            pool: pool.clone(),
            jwt_secret: jwt_secret.to_string(),
        };
        state
            .run_migrations()
            .await
            .expect("migration should succeed in tests");
        state
    }

    async fn run_migrations(&self) -> Result<(), sqlx::Error> {
        sqlx::migrate!("./migrations").run(&self.pool).await
    }
}

pub fn build_router(state: AppState) -> Router {
    Router::new()
        .route("/health", get(health))
        .route("/auth/register", post(routes::auth::register))
        .route("/auth/login", post(routes::auth::login))
        .route("/users/me", get(routes::users::me))
        .route(
            "/vendors",
            post(routes::vendors::create).get(routes::vendors::list),
        )
        .route("/vendors/nearby", get(routes::vendors::nearby))
        .route("/vendors/search", get(routes::vendors::search))
        .route("/vendors/:id", get(routes::vendors::detail))
        .route("/delivery-persons", post(routes::delivery_persons::create))
        .route(
            "/delivery-persons/available",
            get(routes::delivery_persons::available),
        )
        .route(
            "/delivery-persons/:id/status",
            patch(routes::delivery_persons::update_status),
        )
        .route(
            "/delivery-requests/:id/accept",
            post(routes::delivery_persons::accept_request),
        )
        .route("/chats", post(routes::chat::create))
        .route("/chats/:id", get(routes::chat::detail))
        .route(
            "/chats/:id/messages",
            get(routes::chat::messages).post(routes::chat::send_message),
        )
        .route(
            "/delivery-requests",
            post(routes::delivery_requests::create).get(routes::delivery_requests::list),
        )
        .route(
            "/delivery-requests/:id",
            get(routes::delivery_requests::detail),
        )
        .route(
            "/delivery-requests/:id/status",
            patch(routes::delivery_requests::update_status),
        )
        .route("/payments/create", post(routes::payments::create))
        .route("/payments/:id", get(routes::payments::detail))
        .route("/payments/:id/confirm", post(routes::payments::confirm))
        .route(
            "/location-rules",
            get(routes::location_rules::list).post(routes::location_rules::create),
        )
        .route("/fees", get(routes::fees::list))
        .route("/settlements", post(routes::settlements::create))
        .route("/settlements/:id", get(routes::settlements::detail))
        .route("/admin/safety-flags", get(routes::safety::list))
        .route("/admin/safety-flags/:id", patch(routes::safety::action))
        .with_state(state)
}

async fn health(
    axum::extract::State(state): axum::extract::State<AppState>,
) -> Result<axum::Json<serde_json::Value>, StatusCode> {
    sqlx::query("SELECT 1")
        .execute(&state.pool)
        .await
        .map_err(|_| StatusCode::SERVICE_UNAVAILABLE)?;
    Ok(axum::Json(serde_json::json!({
        "status": "ok",
        "database": "ok"
    })))
}
