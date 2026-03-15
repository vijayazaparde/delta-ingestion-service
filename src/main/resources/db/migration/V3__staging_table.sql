-- 1. Drop the old table (Warning: This deletes existing staging data)
DROP TABLE IF EXISTS staging_customer;

-- 2. Create the parent table
CREATE TABLE staging_customer (
    id SERIAL,
    request_id VARCHAR(50) NOT NULL,
    external_id VARCHAR(100),
    name VARCHAR(255),
    email VARCHAR(255),
    country_code VARCHAR(10),
    status_code VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW() NOT NULL
) PARTITION BY RANGE (created_at);

-- This ensures the system NEVER crashes, even if a new day starts
CREATE TABLE IF NOT EXISTS staging_customer_default
PARTITION OF staging_customer DEFAULT;