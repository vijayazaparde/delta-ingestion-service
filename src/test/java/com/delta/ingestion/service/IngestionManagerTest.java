package com.delta.ingestion.service;

import com.delta.ingestion.BaseIntegrationTest;
import com.delta.ingestion.exception.DatabaseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertThrows;

class IngestionManagerTest extends BaseIntegrationTest {

    @Autowired
    private IngestionManager ingestionManager;

    @Test
    void shouldPreventDuplicateIngestionLocks() {
        String requestId = "lock-test-456";

        // First lock
        ingestionManager.createPendingLock(requestId);

        // Second lock with same ID should throw DatabaseException
        assertThrows(DatabaseException.class, () ->
                ingestionManager.createPendingLock(requestId)
        );
    }
}