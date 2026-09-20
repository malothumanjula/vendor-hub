use axum::{extract::Request, http::StatusCode, middleware::Next, response::Response};

pub async fn auth_guard(request: Request, next: Next) -> Result<Response, StatusCode> {
    let _ = request.headers();
    Ok(next.run(request).await)
}
