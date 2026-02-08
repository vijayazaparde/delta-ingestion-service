CREATE UNIQUE INDEX IF NOT EXISTS ux_customers_external_id
ON customers(external_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_countries_code
ON countries(code);

CREATE UNIQUE INDEX IF NOT EXISTS ux_customer_status_code
ON customer_status(code);
