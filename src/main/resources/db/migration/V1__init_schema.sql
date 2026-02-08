CREATE TABLE countries (
    id BIGSERIAL PRIMARY KEY,
    code TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL
);

CREATE TABLE customer_status (
    id BIGSERIAL PRIMARY KEY,
    code TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL
);

CREATE TABLE customers (
    customer_id BIGSERIAL PRIMARY KEY,
    external_id TEXT UNIQUE NOT NULL,
    name TEXT,
    email TEXT,
    country_id BIGINT REFERENCES countries(id),
    status_id BIGINT REFERENCES customer_status(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
