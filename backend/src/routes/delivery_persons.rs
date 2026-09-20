use axum::{
    extract::{Path, State},
    http::{HeaderMap, StatusCode},
    Json,
};
use serde::Deserialize;
use serde_json::json;
use uuid::Uuid;

use crate::{authenticated_user, AppState};

#[derive(Debug, Deserialize)]
pub struct DeliveryInput {
    pub name: String,
    pub email: String,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
}
#[derive(Debug, Deserialize)]
pub struct StatusInput {
    pub status: String,
}

pub async fn create(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(input): Json<DeliveryInput>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if user.role != "DELIVERY_PERSON" && user.role != "ADMIN" {
        return Err(StatusCode::FORBIDDEN);
    }
    if input.name.trim().is_empty() || !input.email.contains('@') {
        return Err(StatusCode::BAD_REQUEST);
    }
    let id = if user.role == "DELIVERY_PERSON" {
        user.id
    } else {
        Uuid::new_v4().to_string()
    };
    sqlx::query("INSERT INTO delivery_persons (id,name,email,status,latitude,longitude) VALUES (?, ?, ?, 'AVAILABLE', ?, ?)")
        .bind(&id).bind(input.name.trim()).bind(input.email.trim()).bind(input.latitude).bind(input.longitude)
        .execute(&state.pool).await.map_err(|_| StatusCode::CONFLICT)?;
    Ok(Json(
        json!({"id":id,"name":input.name,"email":input.email,"status":"AVAILABLE"}),
    ))
}

pub async fn available(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<Json<serde_json::Value>, StatusCode> {
    authenticated_user(&headers, &state).await?;
    let rows: Vec<(String,String,String,Option<f64>,Option<f64>)> = sqlx::query_as("SELECT id,name,email,latitude,longitude FROM delivery_persons WHERE status = 'AVAILABLE' ORDER BY created_at")
        .fetch_all(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"delivery_persons": rows.into_iter().map(|(id,name,email,latitude,longitude)| json!({"id":id,"name":name,"email":email,"latitude":latitude,"longitude":longitude,"status":"AVAILABLE"})).collect::<Vec<_>>() }),
    ))
}

pub async fn update_status(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
    Json(input): Json<StatusInput>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if !["AVAILABLE", "BUSY", "OFFLINE"].contains(&input.status.as_str()) {
        return Err(StatusCode::BAD_REQUEST);
    }
    let owner: (String,) = sqlx::query_as("SELECT id FROM delivery_persons WHERE id = ?")
        .bind(&id)
        .fetch_optional(&state.pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
        .ok_or(StatusCode::NOT_FOUND)?;
    if user.role != "ADMIN" && user.id != owner.0 {
        return Err(StatusCode::FORBIDDEN);
    }
    sqlx::query("UPDATE delivery_persons SET status = ? WHERE id = ?")
        .bind(&input.status)
        .bind(&id)
        .execute(&state.pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(json!({"id":id,"status":input.status})))
}

pub async fn accept_request(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(request_id): Path<String>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if user.role != "DELIVERY_PERSON" && user.role != "ADMIN" {
        return Err(StatusCode::FORBIDDEN);
    }
    let delivery_id = if user.role == "DELIVERY_PERSON" {
        user.id
    } else {
        return Err(StatusCode::BAD_REQUEST);
    };
    let mut transaction = state
        .pool
        .begin()
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let changed = sqlx::query("UPDATE delivery_requests SET assigned_delivery_person_id = ?, status = 'ASSIGNED' WHERE id = ? AND status = 'PAYMENT_PENDING' AND payment_status = 'PAID' AND assigned_delivery_person_id IS NULL")
        .bind(&delivery_id).bind(&request_id).execute(&mut *transaction).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if changed.rows_affected() != 1 {
        return Err(StatusCode::CONFLICT);
    }
    sqlx::query(
        "UPDATE delivery_persons SET status = 'BUSY' WHERE id = ? AND status = 'AVAILABLE'",
    )
    .bind(&delivery_id)
    .execute(&mut *transaction)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    transaction
        .commit()
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"delivery_request_id":request_id,"delivery_person_id":delivery_id,"status":"ASSIGNED"}),
    ))
}
