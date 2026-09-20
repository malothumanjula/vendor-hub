use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub id: String,
    pub email: String,
    pub name: String,
    pub role: String,
}

impl User {
    pub fn new(id: String, email: String, name: String, role: String) -> Self {
        Self {
            id,
            email,
            name,
            role,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AuthRequest {
    pub email: String,
    pub password: String,
    pub name: Option<String>,
    pub role: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AuthResponse {
    pub token: String,
    pub user: User,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VendorRequest {
    pub name: String,
    pub category: String,
    pub description: String,
    pub location: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeliveryRequestPayload {
    pub customer_id: String,
    pub vendor_id: String,
    pub item_summary: String,
    pub amount: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PaymentCreateRequest {
    pub delivery_request_id: String,
    pub amount: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LoginRequest {
    pub email: String,
    pub password: String,
}

#[derive(Debug, Clone)]
pub struct AuthenticatedUser {
    pub id: String,
    pub role: String,
}
