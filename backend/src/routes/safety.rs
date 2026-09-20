use axum::{
    extract::{Path, State},
    http::{HeaderMap, StatusCode},
    Json,
};
use serde::Deserialize;
use serde_json::json;

use crate::{authenticated_user, AppState};

#[derive(Debug, Deserialize)]
pub struct SafetyAction {
    pub action: String,
}

pub async fn list(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if user.role != "ADMIN" {
        return Err(StatusCode::FORBIDDEN);
    }
    let rows: Vec<(String,String,String,String,String,String)> = sqlx::query_as("SELECT id,chat_id,sender_id,message,reason,action FROM safety_flags ORDER BY created_at DESC")
        .fetch_all(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"flags": rows.into_iter().map(|(id,chat_id,sender_id,message,reason,action)| json!({"id":id,"chat_id":chat_id,"sender_id":sender_id,"message":message,"reason":reason,"action":action})).collect::<Vec<_>>() }),
    ))
}

pub async fn action(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
    Json(input): Json<SafetyAction>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if user.role != "ADMIN" {
        return Err(StatusCode::FORBIDDEN);
    }
    if !["WARNING", "RESTRICT", "SUSPEND", "BAN"].contains(&input.action.as_str()) {
        return Err(StatusCode::BAD_REQUEST);
    }
    let changed = sqlx::query("UPDATE safety_flags SET action = ? WHERE id = ?")
        .bind(&input.action)
        .bind(&id)
        .execute(&state.pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if changed.rows_affected() == 0 {
        return Err(StatusCode::NOT_FOUND);
    }
    sqlx::query("INSERT INTO audit_logs (id, actor_id, action, entity_type, entity_id, details) VALUES (?, ?, 'SAFETY_ACTION', 'SAFETY_FLAG', ?, ?)")
        .bind(uuid::Uuid::new_v4().to_string()).bind(&user.id).bind(&id).bind(&input.action)
        .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(json!({"id": id, "action": input.action})))
}
