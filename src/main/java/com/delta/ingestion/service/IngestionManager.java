package com.delta.ingestion.service;

import com.delta.ingestion.entity.ProcessedRequest;
import com.delta.ingestion.exception.DatabaseException;
import com.delta.ingestion.repository.CustomerRepository;
import com.delta.ingestion.repository.ProcessedRequestRepository;
import com.delta.ingestion.repository.StagingRepository;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionManager {

    private final CustomerRepository customerRepository;
    private final StagingRepository stagingRepository;
    private final ProcessedRequestRepository processedRequestRepository;
    private final JdbcTemplate jdbcTemplate;

    private final Counter recordsInsertedCounter;
    private final Counter recordsSkippedCounter;
    private final Counter recordsFailedCounter;

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    /**
     * Fix for Partitioning Bug: Dynamically creates daily partitions.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void preparePartition(LocalDate date) {
        String partitionName = "staging_customer_" + date.toString().replace("-", "_");
        String start = date.toString();
        String end = date.plusDays(1).toString();

        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s PARTITION OF staging_customer " +
                        "FOR VALUES FROM ('%s') TO ('%s')", partitionName, start, end
        );
        jdbcTemplate.execute(sql);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPendingLock(String requestId) {
        String sql = """
                INSERT INTO processed_requests 
                (request_id, status, started_at, received, inserted, skipped, failed)
                VALUES (?, ?, NOW(), 0, 0, 0, 0)
                ON CONFLICT (request_id) DO NOTHING
                """;

        int rowsAffected = jdbcTemplate.update(sql, requestId, STATUS_IN_PROGRESS);
        if (rowsAffected == 0) {
            throw new DatabaseException("Ingestion already in progress or completed for: " + requestId);
        }
    }

    @Transactional
    public ProcessedRequest finalizeIngestion(String requestId, int received, boolean dryRun, int dupeRecords, int invalidRecords) {
        ProcessedRequest result = processedRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new DatabaseException("Lock record missing for: " + requestId));

        int lookupFailures = customerRepository.countInvalidLookups(requestId);
        int existingInDb = customerRepository.countExisting(requestId);

        int failed = lookupFailures + invalidRecords;
        int skipped = existingInDb + dupeRecords;

        recordsFailedCounter.increment(failed);
        recordsSkippedCounter.increment(skipped);

        int inserted = 0;
        if (!dryRun) {
            inserted = customerRepository.insertDelta(requestId);
            recordsInsertedCounter.increment(inserted);
        }

        result.setReceived(received);
        result.setInserted(inserted);
        result.setSkipped(skipped);
        result.setFailed(failed);
        result.setStatus(STATUS_COMPLETED);
        result.setProcessedAt(Instant.now());

        processedRequestRepository.saveAndFlush(result);
        stagingRepository.cleanup(requestId);

        return result;
    }

    @Transactional
    public void rollbackLock(String requestId) {
        processedRequestRepository.findByRequestId(requestId).ifPresent(processedRequestRepository::delete);
        stagingRepository.cleanup(requestId);
    }
}