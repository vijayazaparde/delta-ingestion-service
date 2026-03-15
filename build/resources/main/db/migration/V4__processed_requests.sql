CREATE TABLE IF NOT EXISTS processed_requests (
    id BIGSERIAL PRIMARY KEY,

    request_id VARCHAR(100) NOT NULL,

    status VARCHAR(30) NOT NULL,

    received INT DEFAULT 0,
    inserted INT DEFAULT 0,
    skipped INT DEFAULT 0,
    failed INT DEFAULT 0,

    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,

    CONSTRAINT uk_request_id UNIQUE (request_id)
);