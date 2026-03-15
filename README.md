# Delta Ingestion Service

A scalable backend service that performs **delta ingestion** of customer data into a relational database.  
The system compares incoming customer data against existing records and inserts only new records, ensuring data integrity and high performance.

---

## 🚀 Overview

This service models **real-world ETL ingestion pipelines** where data continuously arrives and must be merged safely with existing datasets. Key benefits include:
* **No Duplicate Inserts**: Strict enforcement of uniqueness.
* **Lookup Resolution**: Automatic mapping of country and status codes to database IDs.
* **Idempotency**: Safe reprocessing of requests using `X-Request-ID`.
* **High Scalability**: Efficient processing for datasets exceeding 10M+ records.

---

## 🏗️ Architecture

The ingestion pipeline follows a **Staging → Validation → Delta Insert** workflow.



1. **Incoming JSON**: Received via REST API.
2. **Streaming Parser**: Jackson `JsonParser` processes data row-by-row to save memory.
3. **Staging Table**: Data is bulk-loaded into a partitioned staging area.
4. **Lookup Validation**: SQL Joins resolve codes to IDs.
5. **Delta Detection**: Set-based SQL identifies records not yet in production.
6. **Production Insert**: Only new/valid records are persisted.

---

## 🧱 Tech Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.2.2 |
| **Database** | PostgreSQL 15+ |
| **Cache/Lock** | Redis (Memurai for Windows) |
| **Migrations** | Flyway |
| **Data Access** | JdbcTemplate (Batch Processing) |
| **Metrics** | Micrometer |
| **Container** | Docker |
| **Build Tool** | Gradle |

---

## 📦 Database Schema

### 1. customers (Destination)
| Column | Type | Description |
| :--- | :--- | :--- |
| **id** | BIGINT | Primary Key |
| **external_id**| TEXT | Unique customer identifier |
| **country_id** | BIGINT | FK → countries.id |
| **status_id** | BIGINT | FK → customer_status.id |

### 2. staging_customer (Partitioned)
Temporary table used for high-speed bulk ingestion.
* **Unique Constraint**: `(request_id, external_id, created_at)`

---

## ⚙️ Setup Instructions

### Prerequisites
- **Java 21**
- **PostgreSQL 15+**
- **Redis** (Memurai for Windows)
- **Docker** (Optional)

### Install Redis (Memurai) on Windows
1. **Download**: [Memurai Get Started](https://www.memurai.com/get-memurai)
2. **Install**: Run the installer; it runs as a Windows service automatically.
3. **Verify**: Run `memurai-cli ping`. Expected response: `PONG`.

### Local Setup
1. **Clone & Navigate**:
   ```bash
   git clone <repository-url>
   cd delta-ingestion-service

2. **Create Database**:
```sql
CREATE DATABASE assignment_db;

```


3. **Configure Environment**:
* **Windows (PS)**: `$env:SPRING_PROFILES_ACTIVE="local"`
* **Linux/Mac**: `export SPRING_PROFILES_ACTIVE=local`


4. **Run Application**:
```bash
./gradlew bootRun

```



### Docker Setup

```bash
docker-compose up --build

```

---

## 📡 API Documentation

### Ingest Customers

**POST** `/customers/ingest`

**Headers:**
| Header | Value |
| :--- | :--- |
| `X-Request-ID` | Unique UUID (Mandatory) |
| `Content-Type` | `application/json` |

**Sample Request:**

```json
[
  {
    "external_id": "cust_999",
    "name": "Vijay",
    "email": "vj@example.com",
    "country_code": "IN",
    "status_code": "ACTIVE"
  }
]

```

**Sample Response:**

```json
{
  "received": 1,
  "inserted": 1,
  "skipped_existing": 0,
  "failed": 0,
  "time_taken": "116ms",
  "rows_scanned": 1,
  "cache_hit_ratio": 0.0
}

```

---

## 🛠️ Key Strategies

### 1. Delta Detection

Performed using set-based SQL to avoid N+1 queries:

```sql
INSERT INTO customers (...)
SELECT s.* FROM staging_customer s
LEFT JOIN customers c ON s.external_id = c.external_id
WHERE c.external_id IS NULL AND s.request_id = :reqId

```

### 2. Idempotency

* **Request ID Tracking**: Redis/Database locks prevent concurrent processing of the same `X-Request-ID`.
* **Database Constraints**: `ON CONFLICT DO NOTHING` ensures that even if a record slips through, the DB remains consistent.

### 3. Performance

* **Streaming JSON**: Uses Jackson to prevent `OutOfMemoryError` on 100MB+ files.
* **Partitioning**: Staging tables are partitioned by date for instant cleanup.

---

## 🧪 Running Tests

Run all validation scenarios (Stress, Idempotency, Logic):

```bash
./gradlew test

```