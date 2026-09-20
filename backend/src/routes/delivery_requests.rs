use axum::{
    extract::{Path, State},
    http::{HeaderMap, StatusCode},
    Json,
};
use serde::Deserialize;
use serde_json::json;
use uuid::Uuid;

use crate::{authenticated_user, AppState};

const STATES: &[&str] = &[
    "REQUESTED",
    "DISCUSSING",
    "COST_CONFIRMED",
    "PAYMENT_PENDING",
    "ASSIGNED",
    "PURCHASED",
    "COLLECTED",
    "OUT_FOR_DELIVERY",
    "DELIVERED",
    "COMPLETED",
    "CANCELLED",
];

#[derive(Debug, Deserialize)]
pub struct RequestInput {
    pub vendor_id: String,
    pub item_summary: String,
    pub chat_id: Option<String>,
    pub location_type: Option<String>,
}
#[derive(Debug, Deserialize)]
pub struct StatusInput {
    pub status: String,
    pub item_amount: Option<i64>,
    pub delivery_fee: Option<i64>,
    pub platform_fee: Option<i64>,
}

fn transition_allowed(from: &str, to: &str) -> bool {
    matches!(
        (from, to),
        ("REQUESTED", "DISCUSSING")
            | ("REQUESTED", "CANCELLED")
            | ("DISCUSSING", "COST_CONFIRMED")
            | ("DISCUSSING", "CANCELLED")
            | ("COST_CONFIRMED", "PAYMENT_PENDING")
            | ("COST_CONFIRMED", "CANCELLED")
            | ("PAYMENT_PENDING", "ASSIGNED")
            | ("PAYMENT_PENDING", "CANCELLED")
            | ("ASSIGNED", "PURCHASED")
            | ("ASSIGNED", "CANCELLED")
            | ("PURCHASED", "COLLECTED")
            | ("COLLECTED", "OUT_FOR_DELIVERY")
            | ("OUT_FOR_DELIVERY", "DELIVERED")
            | ("DELIVERED", "COMPLETED")
    )
}

fn request_json(
    row: (
        String,
        String,
        String,
        String,
        i64,
        i64,
        i64,
        i64,
        String,
        String,
        Option<String>,
        String,
    ),
) -> serde_json::Value {
    let (
        id,
        customer_id,
        vendor_id,
        item_summary,
        item_amount,
        delivery_fee,
        platform_fee,
        amount,
        status,
        payment_status,
        assigned,
        created_at,
    ) = row;
    json!({"id":id,"customer_id":customer_id,"vendor_id":vendor_id,"item_summary":item_summary,"item_amount":item_amount,"delivery_fee":delivery_fee,"platform_fee":platform_fee,"amount":amount,"status":status,"payment_status":payment_status,"assigned_delivery_person_id":assigned,"created_at":created_at})
}

pub async fn create(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(input): Json<RequestInput>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if user.role != "CUSTOMER" && user.role != "ADMIN" {
        return Err(StatusCode::FORBIDDEN);
    }
    if input.item_summary.trim().is_empty() || input.item_summary.len() > 2000 {
        return Err(StatusCode::BAD_REQUEST);
    }
    let vendor_exists: Option<(String, String)> =
        sqlx::query_as("SELECT id, location_type FROM vendors WHERE id = ? AND status = 'ACTIVE'")
            .bind(&input.vendor_id)
            .fetch_optional(&state.pool)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let Some((_, location_type)) = vendor_exists else {
        return Err(StatusCode::NOT_FOUND);
    };
    let id = Uuid::new_v4().to_string();
    sqlx::query("INSERT INTO delivery_requests (id, customer_id, vendor_id, item_summary, amount, item_amount, delivery_fee, platform_fee, status, payment_status, chat_id) VALUES (?, ?, ?, ?, 0, 0, 0, 0, 'REQUESTED', 'PENDING', ?)")
        .bind(&id).bind(&user.id).bind(&input.vendor_id).bind(input.item_summary.trim()).bind(&input.chat_id)
        .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    sqlx::query("INSERT INTO delivery_request_items (id, delivery_request_id, description, quantity) VALUES (?, ?, ?, 1)")
        .bind(Uuid::new_v4().to_string()).bind(&id).bind(input.item_summary.trim())
        .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(json!({
        "id": id,
        "status": "REQUESTED",
        "item_summary": input.item_summary,
        "location_type": location_type
    })))
}

pub async fn list(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    let (sql, id) = if user.role == "CUSTOMER" {
        ("SELECT id,customer_id,vendor_id,item_summary,item_amount,delivery_fee,platform_fee,amount,status,payment_status,assigned_delivery_person_id,created_at FROM delivery_requests WHERE customer_id = ? ORDER BY created_at DESC", Some(user.id))
    } else if user.role == "VENDOR" {
        ("SELECT id,customer_id,vendor_id,item_summary,item_amount,delivery_fee,platform_fee,amount,status,payment_status,assigned_delivery_person_id,created_at FROM delivery_requests WHERE vendor_id IN (SELECT id FROM vendors WHERE user_id = ?) ORDER BY created_at DESC", Some(user.id))
    } else {
        ("SELECT id,customer_id,vendor_id,item_summary,item_amount,delivery_fee,platform_fee,amount,status,payment_status,assigned_delivery_person_id,created_at FROM delivery_requests ORDER BY created_at DESC", None)
    };
    let mut query = sqlx::query_as::<
        _,
        (
            String,
            String,
            String,
            String,
            i64,
            i64,
            i64,
            i64,
            String,
            String,
            Option<String>,
            String,
        ),
    >(sql);
    if let Some(id) = id {
        query = query.bind(id);
    }
    let rows = query
        .fetch_all(&state.pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"delivery_requests": rows.into_iter().map(request_json).collect::<Vec<_>>() }),
    ))
}

pub async fn detail(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    let row = sqlx::query_as::<_, (String,String,String,String,i64,i64,i64,i64,String,String,Option<String>,String)>("SELECT id,customer_id,vendor_id,item_summary,item_amount,delivery_fee,platform_fee,amount,status,payment_status,assigned_delivery_person_id,created_at FROM delivery_requests WHERE id = ?").bind(&id).fetch_optional(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?.ok_or(StatusCode::NOT_FOUND)?;
    if user.role != "ADMIN"
        && user.id != row.1
        && !(user.role == "VENDOR"
            && sqlx::query("SELECT 1 FROM vendors WHERE id = ? AND user_id = ?")
                .bind(&row.2)
                .bind(&user.id)
                .fetch_optional(&state.pool)
                .await
                .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
                .is_some())
        && user.id != row.10.clone().unwrap_or_default()
    {
        return Err(StatusCode::FORBIDDEN);
    }
    Ok(Json(request_json(row)))
}

pub async fn update_status(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
    Json(input): Json<StatusInput>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if !STATES.contains(&input.status.as_str()) {
        return Err(StatusCode::BAD_REQUEST);
    }
    let current: (String,String,String,String,String) = sqlx::query_as("SELECT status,customer_id,vendor_id,payment_status,assigned_delivery_person_id FROM delivery_requests WHERE id = ?").bind(&id).fetch_optional(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?.ok_or(StatusCode::NOT_FOUND)?;
    let vendor_owner: Option<(String,)> =
        sqlx::query_as("SELECT user_id FROM vendors WHERE id = ?")
            .bind(&current.2)
            .fetch_optional(&state.pool)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let allowed_actor = user.role == "ADMIN"
        || user.id == current.1
        || vendor_owner
            .as_ref()
            .map(|v| v.0 == user.id)
            .unwrap_or(false)
        || current.4.as_deref() == Some(&user.id);
    if !allowed_actor {
        return Err(StatusCode::FORBIDDEN);
    }
    if !transition_allowed(&current.0, &input.status) {
        return Err(StatusCode::CONFLICT);
    }
    if input.status == "COST_CONFIRMED"
        && user.role != "VENDOR"
        && user.role != "DELIVERY_PERSON"
        && user.role != "ADMIN"
    {
        return Err(StatusCode::FORBIDDEN);
    }
    if input.status == "ASSIGNED" && user.role != "DELIVERY_PERSON" && user.role != "ADMIN" {
        return Err(StatusCode::FORBIDDEN);
    }
    if input.status == "COST_CONFIRMED"
        && (input.item_amount.unwrap_or(0) < 0
            || input.delivery_fee.unwrap_or(0) < 0
            || input.platform_fee.unwrap_or(0) < 0)
    {
        return Err(StatusCode::BAD_REQUEST);
    }
    let item_amount = input.item_amount.unwrap_or(0);
    let delivery_fee = input.delivery_fee.unwrap_or(0);
    let platform_fee = input.platform_fee.unwrap_or(0);
    let total = item_amount + delivery_fee + platform_fee;
    sqlx::query("UPDATE delivery_requests SET status = ?, item_amount = CASE WHEN ? = 'COST_CONFIRMED' THEN ? ELSE item_amount END, delivery_fee = CASE WHEN ? = 'COST_CONFIRMED' THEN ? ELSE delivery_fee END, platform_fee = CASE WHEN ? = 'COST_CONFIRMED' THEN ? ELSE platform_fee END, amount = CASE WHEN ? = 'COST_CONFIRMED' THEN ? ELSE amount END, payment_status = CASE WHEN ? = 'PAYMENT_PENDING' THEN 'PENDING' ELSE payment_status END WHERE id = ?")
        .bind(&input.status).bind(&input.status).bind(item_amount).bind(&input.status).bind(delivery_fee).bind(&input.status).bind(platform_fee).bind(&input.status).bind(total).bind(&input.status).bind(&id)
        .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"id": id, "status": input.status, "amount": total}),
    ))
}
