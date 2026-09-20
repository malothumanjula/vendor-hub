use axum::{
    extract::State,
    http::{HeaderMap, StatusCode},
    Json,
};
use serde_json::json;

use crate::{authenticated_user, AppState};

pub async fn me(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let auth = authenticated_user(&headers, &state).await?;
    let user: (String, String, String, String) =
        sqlx::query_as("SELECT id, email, name, role FROM users WHERE id = ?")
            .bind(&auth.id)
            .fetch_optional(&state.pool)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
            .ok_or(StatusCode::NOT_FOUND)?;
    Ok(Json(
        json!({"id": user.0, "email": user.1, "name": user.2, "role": user.3}),
    ))
}
