use axum::{
    extract::State,
    http::{HeaderMap, StatusCode},
    Json,
};
use serde::Deserialize;
use serde_json::json;
use uuid::Uuid;

use crate::{authenticated_user, AppState};

#[derive(Debug, Deserialize)]
pub struct LocationRuleInput {
    pub name: String,
    pub rule_type: String,
    pub flow: String,
}

pub async fn list(State(state): State<AppState>) -> Result<Json<serde_json::Value>, StatusCode> {
    let rows: Vec<(String, String, String, String)> =
        sqlx::query_as("SELECT id, name, type, flow FROM location_rules ORDER BY created_at DESC")
            .fetch_all(&state.pool)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(json!(rows
        .into_iter()
        .map(
            |(id, name, rule_type, flow)| json!({"id":id,"name":name,"type":rule_type,"flow":flow})
        )
        .collect::<Vec<_>>())))
}

pub async fn create(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(input): Json<LocationRuleInput>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if user.role != "ADMIN" {
        return Err(StatusCode::FORBIDDEN);
    }
    if ![
        "NORMAL_VENDOR",
        "ASSISTED_MARKET",
        "RESTRICTED_LISTING",
        "DELIVERY_ENABLED",
    ]
    .contains(&input.rule_type.as_str())
        || input.name.trim().is_empty()
    {
        return Err(StatusCode::BAD_REQUEST);
    }
    let id = Uuid::new_v4().to_string();
    sqlx::query("INSERT INTO location_rules (id, name, type, flow) VALUES (?, ?, ?, ?)")
        .bind(&id)
        .bind(input.name.trim())
        .bind(&input.rule_type)
        .bind(input.flow.trim())
        .execute(&state.pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"id":id,"name":input.name,"type":input.rule_type,"flow":input.flow}),
    ))
}
