use std::env;

pub struct AppConfig {
    pub database_url: String,
    pub jwt_secret: String,
    pub host: String,
    pub port: u16,
    pub payment_test_mode: bool,
}

impl AppConfig {
    pub fn from_env() -> Self {
        Self {
            database_url: env::var("DATABASE_URL")
                .unwrap_or_else(|_| "sqlite://data/app.db".to_string()),
            jwt_secret: env::var("JWT_SECRET")
                .unwrap_or_else(|_| "change-me-in-production".to_string()),
            host: env::var("SERVER_HOST").unwrap_or_else(|_| "0.0.0.0".to_string()),
            port: env::var("PORT")
                .or_else(|_| env::var("SERVER_PORT"))
                .unwrap_or_else(|_| "8080".to_string())
                .parse()
                .unwrap_or(8080),
            payment_test_mode: env::var("PAYMENT_TEST_MODE")
                .unwrap_or_else(|_| "true".to_string())
                .parse()
                .unwrap_or(true),
        }
    }
}
