INSERT INTO countries(code, name) VALUES
('US', 'United States'),
('IN', 'India')
ON CONFLICT (code) DO NOTHING;

INSERT INTO customer_status(code, name) VALUES
('ACTIVE', 'Active'),
('INACTIVE', 'Inactive')
ON CONFLICT (code) DO NOTHING;