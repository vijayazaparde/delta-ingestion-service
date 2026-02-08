# System Architecture

## Components
- REST Controller
- Ingestion Service
- Repository Layer
- PostgreSQL Database
- Flyway Migration Layer

## Flow
1. API receives batch
2. Chunking applied
3. External IDs extracted
4. Existing records fetched in bulk
5. Delta computed in-memory
6. Lookup tables cached
7. DTO → Entity mapping
8. Bulk insert
9. Transaction commit

## Design Principles
- Insert-only delta ingestion
- Idempotent processing
- Bulk operations
- Lookup caching
- Indexed queries
- Transaction boundaries
- Referential integrity
- Scalable batch processing

## Performance Strategy
- DB indexes
- IN queries for diff
- HashSet lookups
- Bulk saveAll
- Chunk processing
- Lookup caching

## Concurrency Handling
- Unique constraint on external_id
- Transaction isolation
- Safe duplicate prevention
