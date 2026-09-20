use axum::{
    extract::{Path, State},
    http::{HeaderMap, StatusCode},
    Json,
};
use regex::Regex;
use serde::Deserialize;
use serde_json::json;
use uuid::Uuid;

use crate::{authenticated_user, AppState};

#[derive(Debug, Deserialize)]
pub struct ChatInput {
    pub vendor_id: String,
    pub delivery_person_id: Option<String>,
    pub delivery_request_id: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct MessageInput {
    pub message: String,
}

async fn can_access(
    state: &AppState,
    user_id: &str,
    role: &str,
    chat_id: &str,
) -> Result<bool, StatusCode> {
    if role == "ADMIN" {
        return Ok(true);
    }
    let row: Option<(String, String, Option<String>, Option<String>)> = sqlx::query_as("SELECT customer_id, vendor_id, delivery_person_id, delivery_request_id FROM chats WHERE id = ?")
        .bind(chat_id).fetch_optional(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let Some((customer, vendor, delivery, _request)) = row else {
        return Err(StatusCode::NOT_FOUND);
    };
    let vendor_owner: Option<(String,)> =
        sqlx::query_as("SELECT user_id FROM vendors WHERE id = ?")
            .bind(vendor)
            .fetch_optional(&state.pool)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(user_id == customer
        || (role == "VENDOR"
            && vendor_owner
                .as_ref()
                .map(|owner| owner.0 == user_id)
                .unwrap_or(false))
        || delivery.as_deref() == Some(user_id))
}

fn suspicious(message: &str) -> bool {
    Regex::new(r"(?i)(upi\s*id|paytm|phonepe|google\s*pay|gpay|qr\s*code|pay\s+(outside|direct)|bank\s*(account|details)|https?://|\b[6-9][0-9]{9}\b|@[a-z0-9._-]+)").unwrap().is_match(message)
}

pub async fn create(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(input): Json<ChatInput>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if user.role != "CUSTOMER"
        && user.role != "VENDOR"
        && user.role != "DELIVERY_PERSON"
        && user.role != "ADMIN"
    {
        return Err(StatusCode::FORBIDDEN);
    }
    let vendor_id = input.vendor_id;
    let vendor_exists: Option<(String,)> =
        sqlx::query_as("SELECT id FROM vendors WHERE id = ? AND status = 'ACTIVE'")
            .bind(&vendor_id)
            .fetch_optional(&state.pool)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if vendor_exists.is_none() {
        return Err(StatusCode::NOT_FOUND);
    }
    let customer_id = if user.role == "CUSTOMER" {
        user.id.clone()
    } else {
        let request_id = input
            .delivery_request_id
            .as_deref()
            .ok_or(StatusCode::BAD_REQUEST)?;
        sqlx::query_as::<_, (String,)>("SELECT customer_id FROM delivery_requests WHERE id = ?")
            .bind(request_id)
            .fetch_optional(&state.pool)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
            .ok_or(StatusCode::NOT_FOUND)?
            .0
    };
    let id = Uuid::new_v4().to_string();
    sqlx::query("INSERT INTO chats (id, customer_id, vendor_id, delivery_person_id, delivery_request_id) VALUES (?, ?, ?, ?, ?)")
        .bind(&id).bind(&customer_id).bind(&vendor_id).bind(&input.delivery_person_id).bind(&input.delivery_request_id)
        .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"id": id, "customer_id": customer_id, "vendor_id": vendor_id, "delivery_person_id": input.delivery_person_id, "delivery_request_id": input.delivery_request_id}),
    ))
}

pub async fn detail(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if !can_access(&state, &user.id, &user.role, &id).await? {
        return Err(StatusCode::FORBIDDEN);
    }
    let row: (String,String,String,Option<String>,Option<String>,String) = sqlx::query_as("SELECT id,customer_id,vendor_id,delivery_person_id,delivery_request_id,created_at FROM chats WHERE id = ?").bind(&id).fetch_one(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"id": row.0, "customer_id": row.1, "vendor_id": row.2, "delivery_person_id": row.3, "delivery_request_id": row.4, "created_at": row.5}),
    ))
}

pub async fn messages(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if !can_access(&state, &user.id, &user.role, &id).await? {
        return Err(StatusCode::FORBIDDEN);
    }
    let rows: Vec<(String,String,String,String,String,String)> = sqlx::query_as("SELECT id,sender_id,sender_role,message,safety_status,created_at FROM messages WHERE chat_id = ? ORDER BY created_at ASC")
        .bind(&id).fetch_all(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"messages": rows.into_iter().map(|(id,sender_id,sender_role,message,safety_status,created_at)| json!({"id":id,"sender_id":sender_id,"sender_role":sender_role,"message":message,"safety_status":safety_status,"created_at":created_at})).collect::<Vec<_>>() }),
    ))
}

pub async fn send_message(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
    Json(input): Json<MessageInput>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if !can_access(&state, &user.id, &user.role, &id).await? {
        return Err(StatusCode::FORBIDDEN);
    }
    let message = input.message.trim();
    if message.is_empty() || message.len() > 2000 {
        return Err(StatusCode::BAD_REQUEST);
    }
    if suspicious(message) {
        let flag_id = Uuid::new_v4().to_string();
        sqlx::query("INSERT INTO safety_flags (id, chat_id, sender_id, message, reason) VALUES (?, ?, ?, ?, ?)")
            .bind(&flag_id).bind(&id).bind(&user.id).bind(message).bind("external_payment_instruction")
            .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
        sqlx::query("INSERT INTO audit_logs (id, actor_id, action, entity_type, entity_id, details) VALUES (?, ?, 'BLOCK_MESSAGE', 'SAFETY_FLAG', ?, ?)")
            .bind(Uuid::new_v4().to_string()).bind(&user.id).bind(&flag_id).bind("external payment instruction blocked")
            .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
        return Err(StatusCode::UNPROCESSABLE_ENTITY);
    }
    let message_id = Uuid::new_v4().to_string();
    sqlx::query("INSERT INTO messages (id, chat_id, sender_id, sender_role, message, safety_status) VALUES (?, ?, ?, ?, ?, 'ALLOWED')")
        .bind(&message_id).bind(&id).bind(&user.id).bind(&user.role).bind(message)
        .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"id": message_id, "chat_id": id, "sender_id": user.id, "sender_role": user.role, "message": message, "safety_status": "ALLOWED"}),
    ))
}
