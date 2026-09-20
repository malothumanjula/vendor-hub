use axum::{
    extract::{Path, Query, State},
    http::{HeaderMap, StatusCode},
    Json,
};
use serde::Deserialize;
use serde_json::json;
use uuid::Uuid;

use crate::{authenticated_user, AppState};

#[derive(Debug, Deserialize)]
pub struct VendorInput {
    pub name: String,
    pub category: String,
    pub description: String,
    pub location: String,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub location_type: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct SearchQuery {
    pub q: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct NearbyQuery {
    pub latitude: f64,
    pub longitude: f64,
    pub radius_km: Option<f64>,
}

type VendorRow = (
    String,
    Option<String>,
    String,
    String,
    String,
    String,
    Option<f64>,
    Option<f64>,
    String,
    String,
    String,
    String,
);

fn vendor_json(row: VendorRow, distance: Option<f64>) -> serde_json::Value {
    let (
        id,
        user_id,
        name,
        category,
        description,
        location,
        latitude,
        longitude,
        status,
        location_type,
        created_at,
        updated_at,
    ) = row;
    json!({"id": id, "user_id": user_id, "name": name, "category": category, "description": description, "location": location, "latitude": latitude, "longitude": longitude, "status": status, "location_type": location_type, "created_at": created_at, "updated_at": updated_at, "distance_km": distance})
}

fn valid_input(input: &VendorInput) -> bool {
    !input.name.trim().is_empty()
        && !input.category.trim().is_empty()
        && !input.description.trim().is_empty()
        && !input.location.trim().is_empty()
        && input
            .latitude
            .map(|v| (-90.0..=90.0).contains(&v))
            .unwrap_or(true)
        && input
            .longitude
            .map(|v| (-180.0..=180.0).contains(&v))
            .unwrap_or(true)
}

pub async fn create(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(input): Json<VendorInput>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = authenticated_user(&headers, &state).await?;
    if user.role != "VENDOR" && user.role != "ADMIN" {
        return Err(StatusCode::FORBIDDEN);
    }
    if !valid_input(&input) {
        return Err(StatusCode::BAD_REQUEST);
    }
    let id = Uuid::new_v4().to_string();
    let location_type = input.location_type.unwrap_or_else(|| "NORMAL".to_string());
    sqlx::query("INSERT INTO vendors (id, user_id, name, category, description, location, latitude, longitude, status, location_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)")
        .bind(&id).bind(&user.id).bind(&input.name).bind(&input.category).bind(&input.description).bind(&input.location).bind(input.latitude).bind(input.longitude).bind(&location_type)
        .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    sqlx::query("INSERT INTO locations (id, name, latitude, longitude, location_type) VALUES (?, ?, ?, ?, ?)")
        .bind(&id).bind(&input.location).bind(input.latitude).bind(input.longitude).bind(&location_type)
        .execute(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if location_type == "ASSISTED_MARKET" {
        sqlx::query("INSERT INTO assisted_locations (location_id, enabled) VALUES (?, 1)")
            .bind(&id)
            .execute(&state.pool)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    }
    let row = sqlx::query_as::<_, VendorRow>("SELECT id,user_id,name,category,description,location,latitude,longitude,status,location_type,created_at,updated_at FROM vendors WHERE id = ?")
        .bind(&id).fetch_one(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(vendor_json(row, None)))
}

pub async fn list(State(state): State<AppState>) -> Result<Json<serde_json::Value>, StatusCode> {
    let rows = sqlx::query_as::<_, VendorRow>("SELECT id,user_id,name,category,description,location,latitude,longitude,status,location_type,created_at,updated_at FROM vendors WHERE status = 'ACTIVE' ORDER BY created_at DESC")
        .fetch_all(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(json!({
        "vendors": rows
            .into_iter()
            .map(|r| vendor_json(r, None))
            .collect::<Vec<_>>()
    })))
}

pub async fn detail(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let row = sqlx::query_as::<_, VendorRow>("SELECT id,user_id,name,category,description,location,latitude,longitude,status,location_type,created_at,updated_at FROM vendors WHERE id = ?")
        .bind(id).fetch_optional(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?.ok_or(StatusCode::NOT_FOUND)?;
    Ok(Json(vendor_json(row, None)))
}

pub async fn search(
    State(state): State<AppState>,
    Query(query): Query<SearchQuery>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let term = query.q.unwrap_or_default().trim().to_string();
    if term.is_empty() {
        return Err(StatusCode::BAD_REQUEST);
    }
    let pattern = format!("%{}%", term);
    let rows = sqlx::query_as::<_, VendorRow>("SELECT id,user_id,name,category,description,location,latitude,longitude,status,location_type,created_at,updated_at FROM vendors WHERE status = 'ACTIVE' AND (name LIKE ? OR category LIKE ? OR description LIKE ?) ORDER BY name")
        .bind(&pattern).bind(&pattern).bind(&pattern).fetch_all(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(
        json!({"results": rows.into_iter().map(|r| vendor_json(r, None)).collect::<Vec<_>>() }),
    ))
}

pub async fn nearby(
    State(state): State<AppState>,
    Query(query): Query<NearbyQuery>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    if !(-90.0..=90.0).contains(&query.latitude) || !(-180.0..=180.0).contains(&query.longitude) {
        return Err(StatusCode::BAD_REQUEST);
    }
    let radius = query.radius_km.unwrap_or(10.0).clamp(0.1, 100.0);
    let rows = sqlx::query_as::<_, VendorRow>("SELECT id,user_id,name,category,description,location,latitude,longitude,status,location_type,created_at,updated_at FROM vendors WHERE status = 'ACTIVE' AND latitude IS NOT NULL AND longitude IS NOT NULL")
        .fetch_all(&state.pool).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let vendors = rows
        .into_iter()
        .filter_map(|row| {
            let lat = row.6?;
            let lon = row.7?;
            let dlat = (lat - query.latitude).to_radians();
            let dlon = (lon - query.longitude).to_radians();
            let a = (dlat / 2.0).sin().powi(2)
                + query.latitude.to_radians().cos()
                    * lat.to_radians().cos()
                    * (dlon / 2.0).sin().powi(2);
            let distance = 6371.0 * 2.0 * a.sqrt().asin();
            (distance <= radius).then(|| vendor_json(row, Some(distance)))
        })
        .collect::<Vec<_>>();
    Ok(Json(json!({"vendors": vendors})))
}
