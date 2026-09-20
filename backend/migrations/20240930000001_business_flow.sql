ALTER TABLE vendors ADD COLUMN user_id TEXT;
ALTER TABLE vendors ADD COLUMN location_type TEXT NOT NULL DEFAULT 'NORMAL';
ALTER TABLE vendors ADD COLUMN updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE chats ADD COLUMN delivery_person_id TEXT;
ALTER TABLE chats ADD COLUMN delivery_request_id TEXT;

ALTER TABLE delivery_requests ADD COLUMN delivery_fee INTEGER NOT NULL DEFAULT 0;
ALTER TABLE delivery_requests ADD COLUMN platform_fee INTEGER NOT NULL DEFAULT 0;
ALTER TABLE delivery_requests ADD COLUMN item_amount INTEGER NOT NULL DEFAULT 0;
ALTER TABLE delivery_requests ADD COLUMN assigned_delivery_person_id TEXT;
ALTER TABLE delivery_requests ADD COLUMN chat_id TEXT;

ALTER TABLE payments ADD COLUMN item_amount INTEGER NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN delivery_fee INTEGER NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN platform_fee INTEGER NOT NULL DEFAULT 0;

ALTER TABLE chat_messages ADD COLUMN safety_status TEXT NOT NULL DEFAULT 'ALLOWED';

CREATE TABLE IF NOT EXISTS locations (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    latitude REAL,
    longitude REAL,
    location_type TEXT NOT NULL DEFAULT 'NORMAL_VENDOR',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS messages (
    id TEXT PRIMARY KEY,
    chat_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    sender_role TEXT NOT NULL DEFAULT 'CUSTOMER',
    message TEXT NOT NULL,
    safety_status TEXT NOT NULL DEFAULT 'ALLOWED',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(chat_id) REFERENCES chats(id)
);

CREATE TABLE IF NOT EXISTS delivery_request_items (
    id TEXT PRIMARY KEY,
    delivery_request_id TEXT NOT NULL,
    description TEXT NOT NULL,
    quantity REAL NOT NULL DEFAULT 1,
    item_amount INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(delivery_request_id) REFERENCES delivery_requests(id)
);

CREATE TABLE IF NOT EXISTS platform_fees (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    amount INTEGER NOT NULL DEFAULT 0,
    percentage REAL NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS assisted_locations (
    location_id TEXT PRIMARY KEY,
    enabled INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY(location_id) REFERENCES locations(id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id TEXT PRIMARY KEY,
    actor_id TEXT,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT,
    details TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS safety_flags (
    id TEXT PRIMARY KEY,
    chat_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    message TEXT NOT NULL,
    reason TEXT NOT NULL,
    action TEXT NOT NULL DEFAULT 'BLOCKED',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS settlements (
    id TEXT PRIMARY KEY,
    delivery_request_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    role TEXT NOT NULL,
    amount INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT OR IGNORE INTO platform_fees (id, name, amount, percentage) VALUES
    ('delivery-fee', 'delivery_fee', 30, 0),
    ('platform-fee', 'platform_fee', 9, 0);