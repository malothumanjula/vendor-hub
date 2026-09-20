# StreetLink

**Street Vendor Discovery & Intelligent Delivery**

StreetLink is a location-based app that helps customers discover and connect with local street vendors nearby. Customers can find vendors using Google Maps, chat with them about the items they need, and request delivery through the platform.

## Overview

This workspace contains an Android app for a hyperlocal street-vendor discovery and delivery platform and a Rust/Axum backend using SQLite for local development.

## Android app architecture

- App entry point: `app/src/main/java/com/vendorapp/MainActivity.kt`
- UI state: `app/src/main/java/com/vendorapp/ui/AppViewModel.kt`
- Navigation and screens: `app/src/main/java/com/vendorapp/ui/Navigation.kt`
- Reusable UI: `app/src/main/java/com/vendorapp/ui/components/Components.kt`
- Data and mock models: `app/src/main/java/com/vendorapp/data/`

## Backend structure

The backend is organized under `backend/` with a modular layout matching the requested roadmap:

- `backend/src/main.rs`
- `backend/src/config/mod.rs`
- `backend/src/routes/`
- `backend/src/models/mod.rs`
- `backend/src/services/`
- `backend/src/repositories/`
- `backend/src/middleware/mod.rs`

## Android setup

1. Install Android Studio with SDK 35 and JDK 17.
2. Open the project in Android Studio or the workspace folder.
3. Ensure `local.properties` points to your Android SDK path.
4. Run the app from Android Studio or with:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
./gradlew app:installDebug
```

## Backend setup

Install Rust and the Windows MSVC prerequisites in PowerShell. Visual Studio Build Tools must include the C++ workload and Windows SDK:

```powershell
winget install --id Rustlang.Rustup --source winget -e
winget install --id Microsoft.VisualStudio.2022.BuildTools -e --override "--wait --passive --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended"
```

Open **Developer PowerShell for VS 2022** after installation so `cl.exe` and `link.exe` are available. Verify:

```powershell
where.exe rustc
where.exe cargo
where.exe cl
where.exe link
```

Create the environment file and start the backend:

```powershell
Copy-Item .env.example backend/.env
cd backend
cargo run
```

The first start creates `backend/data/app.db` and runs SQLx migrations automatically. The API listens on `http://127.0.0.1:8080`.

Run backend checks:

```powershell
cargo fmt --check
cargo check
cargo test
```

## SQLite setup

- No separate database server is required for local development.
- The schema is in `backend/migrations/20240930000000_init.sql`.
- Set `DATABASE_URL=sqlite://data/app.db` to use the default local database path.

## Environment variables

```env
DATABASE_URL=sqlite://data/app.db
JWT_SECRET=change-me-in-production
SERVER_HOST=127.0.0.1
SERVER_PORT=8080
PAYMENT_TEST_MODE=true
PAYMENT_TEST_KEY=local-test-only
```

## Google Maps configuration

- Store your API key in an environment variable or secure config file.
- Add the key in the Android app build when the Google Maps feature is enabled.

## Payment test configuration

- Use a sandbox/test provider to validate payment flows.
- Do not commit real credentials.

## Backend database and migrations

- SQLx uses SQLite and the `DATABASE_URL` path above.
- Migrations live in `backend/migrations/` and execute during `AppState::new`.
- Required data areas include users, vendors, delivery people, locations, chats, messages, delivery requests/items, payments, fees, settlements, location rules, assisted locations, safety flags, and audit logs.
- Inspect the API contract in [backend/API.md](backend/API.md).

## API overview

The health check is `GET http://127.0.0.1:8080/health`. The complete endpoint contract, roles, request bodies, responses, and errors is in [backend/API.md](backend/API.md).

## Test payment mode

`PAYMENT_TEST_MODE=true` enables the local test provider. It is deliberately not a live payment gateway. Payment state changes to `PAID` only through the backend `/payments/{id}/confirm` endpoint.

## Test accounts

There are no committed credentials. Register local users with `/auth/register`, using roles `CUSTOMER`, `VENDOR`, `DELIVERY_PERSON`, or `ADMIN` for development testing.
