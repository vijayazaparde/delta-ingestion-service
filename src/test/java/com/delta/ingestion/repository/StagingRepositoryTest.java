package com.delta.ingestion.repository;

import com.delta.ingestion.BaseIntegrationTest;
import com.delta.ingestion.dto.IncomingCustomerDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StagingRepositoryTest extends BaseIntegrationTest {

    @Autowired private StagingRepository stagingRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void shouldHandleSpecialCharsInCopy() {
        String reqId = "test-req-1";
        OffsetDateTime now = OffsetDateTime.now(); // Create the timestamp for partitioning

        List<IncomingCustomerDTO> batch = List.of(
                IncomingCustomerDTO.builder()
                        .externalId("EXT1")
                        .name("John\tDoe\nLineBreak")
                        .email("test@delta.com")
                        .build()
        );

        // Act: Pass 'now' as the second argument
        stagingRepository.copyInsert(reqId, now, batch);

        // Assert
        String savedName = jdbcTemplate.queryForObject(
                "SELECT name FROM staging_customer WHERE request_id = ?", String.class, reqId);

        // verify clean() method worked: Tab/Newline replaced by spaces
        assertEquals("John Doe LineBreak", savedName);
    }
}