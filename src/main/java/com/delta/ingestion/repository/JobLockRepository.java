package com.delta.ingestion.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobLockRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean acquireLock() {

        Boolean result = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_lock(99999)",
                Boolean.class
        );

        return Boolean.TRUE.equals(result);
    }

    public void releaseLock() {

        jdbcTemplate.queryForObject(
                "SELECT pg_advisory_unlock(99999)",
                Boolean.class
        );
    }
}
