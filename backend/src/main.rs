use std::net::SocketAddr;

use dotenvy::dotenv;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

#[tokio::main]
async fn main() {
    dotenv().ok();
    tracing_subscriber::registry()
        .with(tracing_subscriber::EnvFilter::new("info"))
        .with(tracing_subscriber::fmt::layer())
        .init();

    let config = street_vendor_backend::config::AppConfig::from_env();
    let state = street_vendor_backend::AppState::new(&config.database_url, &config.jwt_secret)
        .await
        .expect("failed to initialize SQLite database");
    tracing::info!(
        "Payment provider mode: {}",
        if config.payment_test_mode {
            "TEST"
        } else {
            "LIVE CONFIGURATION REQUIRED"
        }
    );

    let app = street_vendor_backend::build_router(state);
    let addr = format!("{}:{}", config.host, config.port)
        .parse::<SocketAddr>()
        .expect("SERVER_HOST and SERVER_PORT must form a valid socket address");

    tracing::info!("Listening on {}", addr);
    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
