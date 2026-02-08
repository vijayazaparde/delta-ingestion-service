# Delta Ingestion Service

A scalable backend service for performing **delta ingestion** of customer data into a relational database.  
The system detects new records, resolves lookup dependencies, ensures idempotency, and inserts only the required data efficiently.

This service is designed to model **real-world data ingestion pipelines** where incoming data must be compared against existing records and only the delta should be applied.

---

## Setup

### Prerequisites
- Java 21
- PostgreSQL
- Gradle


## 🧱 Tech Stack

- Java 21
- Spring Boot 3
- PostgreSQL
- Flyway (DB migrations)
- JPA / Hibernate
- Gradle
- REST API

## 📦 Database Schema

### customers
- id (BIGINT, PK)
- external_id (TEXT, UNIQUE)
- name (TEXT)
- email (TEXT)
- country_id (FK → countries.id)
- status_id (FK → customer_status.id)
- created_at (TIMESTAMP)

### countries
- id (BIGINT, PK)
- code (TEXT, UNIQUE)
- name (TEXT)

### customer_status
- id (BIGINT, PK)
- code (TEXT, UNIQUE)
- name (TEXT)

---
## ⚙️ Setup Instructions
### Steps
1. Clone repo
2. Create database:
   CREATE DATABASE assignment_db;

3. Configure `application.yml`
4. run command: $env:SPRING_PROFILES_ACTIVE="local"
5. Run:
   ./gradlew bootRun

Flyway migrations will auto-run.


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
