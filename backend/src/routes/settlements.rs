use axum::{
    extract::{Path, State},
    http::{HeaderMap, StatusCode},
    Json,
};
use serde::Deserialize;
use serde_json::json;
use uuid::Uuid;

use crate::{authenticated_user, AppState};

#[derive(Deserialize)]
pub struct SettlementRequest {
    pub delivery_request_id: String,
    pub user_id: String,
    pub role: String,
    pub amount: i64,
}

pub async fn create(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(input): Json<SettlementRequest>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if user.role != "ADMIN" && user.id != input.user_id {
        return Err(StatusCode::FORBIDDEN);
    }
    if input.amount < 0 || input.role != "VENDOR" && input.role != "DELIVERY_PERSON" {
        return Err(StatusCode::BAD_REQUEST);
    }
    let id = Uuid::new_v4().to_string();
    sqlx::query("INSERT INTO settlements (id, delivery_request_id, user_id, role, amount) VALUES (?, ?, ?, ?, ?)")
        .bind(&id).bind(&input.delivery_request_id).bind(&input.user_id).bind(&input.role).bind(input.amount)
        .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"id": id, "status": "PENDING", "amount": input.amount}),
    ))
}

pub async fn detail(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    let row: Option<(String,String,String,String,i64,String)> = sqlx::query_as("SELECT id, delivery_request_id, user_id, role, amount, status FROM settlements WHERE id = ?")
        .bind(&id).fetch_optional(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let Some((id, request_id, owner_id, role, amount, status)) = row else {
        return Err(StatusCode::NOT_FOUND);
    };
    if user.role != "ADMIN" && user.id != owner_id {
        return Err(StatusCode::FORBIDDEN);
    }
    Ok(Json(
        json!({"id": id, "delivery_request_id": request_id, "user_id": owner_id, "role": role, "amount": amount, "status": status}),
    ))
}
