use axum::{extract::State, http::StatusCode, Json};
use serde_json::json;

use crate::models::{AuthRequest, User};
use crate::AppState;

use argon2::{
    password_hash::{rand_core::OsRng, PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Argon2,
};
use jsonwebtoken::{Algorithm, EncodingKey, Header};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Serialize, Deserialize)]
struct Claims {
    sub: String,
    email: String,
    role: String,
    exp: usize,
}

fn hash_password(password: &str) -> Result<String, String> {
    let salt = SaltString::generate(&mut OsRng);
    let argon2 = Argon2::default();
    argon2
        .hash_password(password.as_bytes(), &salt)
        .map_err(|err| err.to_string())
        .map(|hash| hash.to_string())
}

fn verify_password(password: &str, expected_hash: &str) -> Result<bool, String> {
    let parsed_hash = PasswordHash::new(expected_hash).map_err(|err| err.to_string())?;
    Ok(Argon2::default()
        .verify_password(password.as_bytes(), &parsed_hash)
        .is_ok())
}

fn create_token(user: &User, jwt_secret: &str) -> Result<String, String> {
    let now = chrono::Utc::now().timestamp() as usize;
    let claims = Claims {
        sub: user.id.clone(),
        email: user.email.clone(),
        role: user.role.clone(),
        exp: now + 86_400,
    };

    jsonwebtoken::encode(
        &Header::new(Algorithm::HS256),
        &claims,
        &EncodingKey::from_secret(jwt_secret.as_bytes()),
    )
    .map_err(|err| err.to_string())
}

pub async fn register(
    State(state): State<AppState>,
    Json(payload): Json<AuthRequest>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    if payload.email.trim().is_empty() || payload.password.trim().is_empty() {
        return Err(StatusCode::BAD_REQUEST);
    }

    let email = payload.email.trim().to_string();
    if !email.contains('@') || payload.password.len() < 8 {
        return Err(StatusCode::BAD_REQUEST);
    }
    let name = payload
        .name
        .unwrap_or_else(|| email.split('@').next().unwrap_or("User").to_string());
    let role = payload
        .role
        .unwrap_or_else(|| "CUSTOMER".to_string())
        .to_uppercase();
    if !["CUSTOMER", "VENDOR", "DELIVERY_PERSON", "ADMIN"].contains(&role.as_str()) {
        return Err(StatusCode::BAD_REQUEST);
    }

    let existing: Option<(String,)> = sqlx::query_as("SELECT email FROM users WHERE email = ?")
        .bind(&email)
        .fetch_optional(&state.pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    if existing.is_some() {
        return Err(StatusCode::CONFLICT);
    }

    let user_id = Uuid::new_v4().to_string();
    let password_hash =
        hash_password(&payload.password).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let user = User::new(user_id.clone(), email.clone(), name, role.clone());

    sqlx::query("INSERT INTO users (id, email, name, role, password_hash) VALUES (?, ?, ?, ?, ?)")
        .bind(&user_id)
        .bind(&email)
        .bind(&user.name)
        .bind(&role)
        .bind(&password_hash)
        .execute(&state.pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let token =
        create_token(&user, &state.jwt_secret).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    Ok(Json(json!({
        "token": token,
        "user": {
            "id": user.id,
            "email": user.email,
            "name": user.name,
            "role": user.role,
        }
    })))
}

pub async fn login(
    State(state): State<AppState>,
    Json(payload): Json<AuthRequest>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    if payload.email.trim().is_empty() || payload.password.trim().is_empty() {
        return Err(StatusCode::BAD_REQUEST);
    }

    let email = payload.email.trim().to_string();
    let record: Option<(String, String, String, String, String)> =
        sqlx::query_as("SELECT id, email, name, password_hash, role FROM users WHERE email = ?")
            .bind(&email)
            .fetch_optional(&state.pool)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let Some((id, _, name, password_hash, role)) = record else {
        return Err(StatusCode::UNAUTHORIZED);
    };

    let valid = verify_password(&payload.password, &password_hash)
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if !valid {
        return Err(StatusCode::UNAUTHORIZED);
    }

    let user = User::new(id, email.clone(), name, role);
    let token =
        create_token(&user, &state.jwt_secret).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    Ok(Json(json!({
        "token": token,
        "user": {
            "id": user.id,
            "email": user.email,
            "name": user.name,
            "role": user.role,
        }
    })))
}
