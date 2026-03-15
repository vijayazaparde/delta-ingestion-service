package com.delta.ingestion.repository;

import com.delta.ingestion.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    @Query(value = """
        SELECT COUNT(*)
        FROM staging_customer s
        LEFT JOIN countries c ON c.code = s.country_code
        LEFT JOIN customer_status st ON st.code = s.status_code
        WHERE s.request_id = :requestId
        AND (c.id IS NULL OR st.id IS NULL)
        """, nativeQuery = true)
    int countInvalidLookups(String requestId);

    @Query(value = """
        SELECT COUNT(*)
        FROM staging_customer s
        JOIN customers existing ON existing.external_id = s.external_id
        WHERE s.request_id = :requestId
        """, nativeQuery = true)
    int countExisting(String requestId);

    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO customers (external_id, name, email, country_id, status_id, created_at)
        SELECT DISTINCT ON (s.external_id) 
               s.external_id, s.name, s.email, c.id, st.id, now()
        FROM staging_customer s
        INNER JOIN countries c ON c.code = s.country_code
        INNER JOIN customer_status st ON st.code = s.status_code
        WHERE s.request_id = :requestId
        AND NOT EXISTS (
            SELECT 1 FROM customers existing 
            WHERE existing.external_id = s.external_id
        )
        ON CONFLICT (external_id) DO NOTHING
        """, nativeQuery = true)
    int insertDelta(String requestId);
}