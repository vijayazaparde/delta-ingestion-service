-- ===========================================================================
-- DATABASE OPTIMIZATION FOR MILLIONS OF RECORDS
-- ===========================================================================

-- 1. STAGING TABLE INDEXES
-- ---------------------------------------------------------------------------

-- UNIQUE INDEX (Mandatory Partition Key inclusion)
-- This prevents duplicates within a request AND satisfies Postgres partitioning rules.
-- We do NOT need a separate index on request_id because it is the leading column here.
CREATE UNIQUE INDEX idx_stage_request_external
ON staging_customer(request_id, external_id, created_at);

-- JOIN OPTIMIZATION INDEX
-- Specifically targets the validation query that checks for invalid country/status codes.
-- Including all three columns allows for an "Index Only Scan" in many cases.
CREATE INDEX IF NOT EXISTS idx_staging_lookup_optim
ON staging_customer(request_id, country_code, status_code);


-- 2. LOOKUP TABLE INDEXES (Production Tables)
-- ---------------------------------------------------------------------------

-- Ensure the 'code' columns used in JOINs during migration are indexed and unique.
CREATE UNIQUE INDEX IF NOT EXISTS idx_countries_code
ON countries(code);

CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_status_code
ON customer_status(code);


-- 3. MONITORING & AUDIT INDEXES
-- ---------------------------------------------------------------------------

-- Speeds up status checks on the processed_requests table (e.g., polling for 'COMPLETED').
CREATE INDEX IF NOT EXISTS idx_processed_req_lookup
ON processed_requests(request_id);

CREATE INDEX IF NOT EXISTS idx_processed_requests_status
ON processed_requests(status);

-- ===========================================================================
-- MIGRATION COMPLETE
-- ===========================================================================