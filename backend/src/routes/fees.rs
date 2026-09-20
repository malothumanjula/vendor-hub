use axum::{extract::State, http::StatusCode, Json};
use serde_json::json;

use crate::AppState;

pub async fn list(State(state): State<AppState>) -> Result<Json<serde_json::Value>, StatusCode> {
    let rows: Vec<(String, i64, f64)> =
        sqlx::query_as("SELECT name, amount, percentage FROM platform_fees WHERE active = 1")
            .fetch_all(&state.pool)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let delivery_fee = rows
        .iter()
        .find(|row| row.0 == "delivery_fee")
        .map(|row| row.1)
        .unwrap_or(30);
    let platform_fee = rows
        .iter()
        .find(|row| row.0 == "platform_fee")
        .map(|row| row.1)
        .unwrap_or(9);
    Ok(Json(
        json!({"delivery_fee": delivery_fee, "platform_fee": platform_fee, "currency": "INR", "fees": rows.into_iter().map(|(name, amount, percentage)| json!({"name": name, "amount": amount, "percentage": percentage})).collect::<Vec<_>>() }),
    ))
}
