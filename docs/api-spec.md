# Delta Ingestion Service

## Setup
1. Clone repo
2. Configure Postgres in application.yml
3. Run:
   ./gradlew bootRun

## API
POST /customers/ingest

### Sample Request
[
{
"external_id": "cust_001",
"name": "Alice",
"email": "alice@example.com",
"country_code": "US",
"status_code": "ACTIVE"
}
]

### Sample Response
{
"received": 1,
"inserted": 1,
"skipped_existing": 0,
"failed": 0
}
