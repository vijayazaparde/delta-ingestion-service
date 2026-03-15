package com.delta.ingestion.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StagingCleanupScheduler {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Smart Cleanup: Only removes data that we are 100% sure is no longer needed.
     */
    @Scheduled(cron = "0 0 * * * *") // Runs every hour
    public void cleanupOrphanedStagingData() {
        log.info("Starting smart cleanup of orphaned staging data...");

        // Strategy: Delete if the request is marked as COMPLETED or FAILED in our log
        // This prevents deleting rows for 'IN_PROGRESS' jobs.
        String sql = """
            DELETE FROM staging_customer 
            WHERE request_id IN (
                SELECT request_id 
                FROM processed_requests 
                WHERE status IN ('COMPLETED', 'FAILED')
            )
            """;

        int rowsDeleted = jdbcTemplate.update(sql);

        int orphanedDeleted = jdbcTemplate.update("""
            DELETE FROM staging_customer sc
            WHERE sc.created_at < NOW() - INTERVAL '24 hours'
            AND NOT EXISTS (
                SELECT 1
                FROM processed_requests pr
                WHERE pr.request_id = sc.request_id
                AND pr.status = 'IN_PROGRESS'
            )
        """);

        log.info("Cleanup finished. Verified rows: {}. Ghost rows: {}.", rowsDeleted, orphanedDeleted);
    }
}