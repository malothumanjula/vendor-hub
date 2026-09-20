# Backend API

Base URL: `http://127.0.0.1:8080`

Protected endpoints use `Authorization: Bearer <JWT>`.
Amounts are integer INR values. The payment provider is `test` when `PAYMENT_TEST_MODE=true`; it is not a live gateway.

## Authentication

### POST `/auth/register`
Auth: no. Body: `{ "email", "password", "name", "role" }`.
Roles: `CUSTOMER`, `VENDOR`, `DELIVERY_PERSON`, `ADMIN`.
Returns success JSON with `token` and `user`; duplicate email returns `409`; invalid input returns `400`.

### POST `/auth/login`
Auth: no. Body: `{ "email", "password" }`.
Returns a JWT and user, `401` for invalid credentials.

### GET `/users/me`
Auth: yes. Returns the authenticated database user. Missing or invalid JWT returns `401`.

## Health

### GET `/health`
Auth: no. Executes `SELECT 1` and returns `{ "status": "ok", "database": "ok" }`; database failure returns `503`.

## Vendors

### POST `/vendors`
Auth: `VENDOR` or `ADMIN`. Body: `name`, `category`, `description`, `location`, optional `latitude`, `longitude`, `location_type`.
Persists the owner and assisted-market location. Invalid data returns `400`.

### GET `/vendors`
Auth: no. Returns active vendors.

### GET `/vendors/{id}`
Auth: no. Returns one vendor or `404`.

### GET `/vendors/search?q=tomato`
Auth: no. Searches active vendor name, category, and description.

### GET `/vendors/nearby?latitude=17.4&longitude=78.4&radius_km=5`
Auth: no. Uses SQLite-loaded coordinates and haversine distance.

## Chat

### POST `/chats`
Auth: yes. Customer body: `{ "vendor_id" }`. Vendor/delivery body must include `delivery_request_id`.
Creates a participant-linked chat.

### GET `/chats/{id}`
Auth: participant or `ADMIN`. Returns chat participants.

### GET `/chats/{id}/messages`
Auth: participant or `ADMIN`. Returns persisted messages.

### POST `/chats/{id}/messages`
Auth: participant or `ADMIN`. Body: `{ "message" }`.
Normal messages are stored. External payment instructions are rejected with `422`, stored in `safety_flags`, and audited.

## Delivery

### POST `/delivery-requests`
Auth: `CUSTOMER` or `ADMIN`. Body: `{ "vendor_id", "item_summary", "chat_id" }`.
The free-form item text is stored in `delivery_request_items`; no catalogue is required.

### GET `/delivery-requests`
Auth: yes. Customers see their requests, vendors see requests for owned vendors, admins see all.

### GET `/delivery-requests/{id}`
Auth: request participant, assigned delivery person, vendor owner, or `ADMIN`.

### PATCH `/delivery-requests/{id}/status`
Auth: role-dependent. Body: `{ "status", "item_amount", "delivery_fee", "platform_fee" }`.
Valid progression is `REQUESTED -> DISCUSSING -> COST_CONFIRMED -> PAYMENT_PENDING -> ASSIGNED -> PURCHASED -> COLLECTED -> OUT_FOR_DELIVERY -> DELIVERED -> COMPLETED`; cancellation is allowed from active early states. The server calculates `amount`.

### POST `/delivery-persons`
Auth: `DELIVERY_PERSON` or `ADMIN`. Body: `name`, `email`, optional coordinates.

### GET `/delivery-persons/available`
Auth: yes. Returns available delivery people.

### PATCH `/delivery-persons/{id}/status`
Auth: owner or `ADMIN`. Body: `{ "status": "AVAILABLE|BUSY|OFFLINE" }`.

### POST `/delivery-requests/{id}/accept`
Auth: `DELIVERY_PERSON`. Atomically assigns one available person to a paid request and prevents duplicate assignment.

## Payments

### POST `/payments/create`
Auth: request customer or `ADMIN`. Body: `{ "delivery_request_id" }`.
Requires a server-confirmed amount and `PAYMENT_PENDING`. Creates a `CREATED` test payment.

### GET `/payments/{id}`
Auth: payment owner or `ADMIN`.

### POST `/payments/{id}/confirm`
Auth: payment owner or `ADMIN`.
The test provider changes the payment to `PAID` and updates the request payment status. Android cannot set `PAID` directly.

## Fees and settlements

### GET `/fees`
Auth: no. Reads active fee records from SQLite, including delivery and platform fees.

### POST `/settlements`
Auth: settlement owner or `ADMIN`. Body: `{ "delivery_request_id", "user_id", "role", "amount" }`.

### GET `/settlements/{id}`
Auth: settlement owner or `ADMIN`.

## Location rules

### GET `/location-rules`
Auth: no. Returns persisted rules: `NORMAL_VENDOR`, `ASSISTED_MARKET`, `RESTRICTED_LISTING`, or `DELIVERY_ENABLED`.

### POST `/location-rules`
Auth: `ADMIN`. Body: `{ "name", "rule_type", "flow" }`.

## Admin safety

### GET `/admin/safety-flags`
Auth: `ADMIN`. Lists blocked payment-safety flags.

### PATCH `/admin/safety-flags/{id}`
Auth: `ADMIN`. Body: `{ "action": "WARNING|RESTRICT|SUSPEND|BAN" }`.
Writes an audit record. Non-admin users receive `403`.
