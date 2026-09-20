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
pub struct PaymentInput {
    pub delivery_request_id: String,
}

pub async fn create(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(input): Json<PaymentInput>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    let request: (String,i64,i64,i64,i64,String,String) = sqlx::query_as("SELECT customer_id,item_amount,delivery_fee,platform_fee,amount,status,payment_status FROM delivery_requests WHERE id = ?")
        .bind(&input.delivery_request_id).fetch_optional(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?.ok_or(StatusCode::NOT_FOUND)?;
    if user.role != "ADMIN" && user.id != request.0 {
        return Err(StatusCode::FORBIDDEN);
    }
    if request.5 != "PAYMENT_PENDING" && request.5 != "COST_CONFIRMED" {
        return Err(StatusCode::CONFLICT);
    }
    if request.1 <= 0 || request.4 <= 0 {
        return Err(StatusCode::CONFLICT);
    }
    let id = Uuid::new_v4().to_string();
    sqlx::query("INSERT INTO payments (id, delivery_request_id, amount, item_amount, delivery_fee, platform_fee, provider, status) VALUES (?, ?, ?, ?, ?, ?, 'test', 'CREATED')")
        .bind(&id).bind(&input.delivery_request_id).bind(request.4).bind(request.1).bind(request.2).bind(request.3)
        .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    sqlx::query("UPDATE delivery_requests SET status = 'PAYMENT_PENDING' WHERE id = ? AND status = 'COST_CONFIRMED'").bind(&input.delivery_request_id).execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"id":id,"delivery_request_id":input.delivery_request_id,"item_amount":request.1,"delivery_fee":request.2,"platform_fee":request.3,"amount":request.4,"status":"CREATED","provider":"test"}),
    ))
}

pub async fn detail(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    let row: (String,String,i64,i64,i64,i64,String,String) = sqlx::query_as("SELECT id,delivery_request_id,amount,item_amount,delivery_fee,platform_fee,status,provider FROM payments WHERE id = ?")
        .bind(&id).fetch_optional(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?.ok_or(StatusCode::NOT_FOUND)?;
    let owner: (String,) = sqlx::query_as("SELECT customer_id FROM delivery_requests WHERE id = ?")
        .bind(&row.1)
        .fetch_one(&state.pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if user.role != "ADMIN" && user.id != owner.0 {
        return Err(StatusCode::FORBIDDEN);
    }
    Ok(Json(
        json!({"id":row.0,"delivery_request_id":row.1,"amount":row.2,"item_amount":row.3,"delivery_fee":row.4,"platform_fee":row.5,"status":row.6,"provider":row.7}),
    ))
}

pub async fn confirm(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    let payment: (String, i64, String) =
        sqlx::query_as("SELECT delivery_request_id,amount,status FROM payments WHERE id = ?")
            .bind(&id)
            .fetch_optional(&state.pool)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
            .ok_or(StatusCode::NOT_FOUND)?;
    let owner: (String,) = sqlx::query_as("SELECT customer_id FROM delivery_requests WHERE id = ?")
        .bind(&payment.0)
        .fetch_one(&state.pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if user.role != "ADMIN" && user.id != owner.0 {
        return Err(StatusCode::FORBIDDEN);
    }
    if payment.2 == "PAID" {
        return Ok(Json(json!({"id":id,"status":"PAID"})));
    }
    if payment.2 != "CREATED" && payment.2 != "PENDING" {
        return Err(StatusCode::CONFLICT);
    }
    let mut transaction = state
        .pool
        .begin()
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    sqlx::query("UPDATE payments SET status = 'PAID' WHERE id = ?")
        .bind(&id)
        .execute(&mut *transaction)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    sqlx::query("UPDATE delivery_requests SET payment_status = 'PAID' WHERE id = ? AND status = 'PAYMENT_PENDING'").bind(&payment.0).execute(&mut *transaction).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    transaction
        .commit()
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"id":id,"status":"PAID","amount":payment.1,"provider":"test"}),
    ))
}
