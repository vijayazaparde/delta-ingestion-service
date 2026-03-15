package com.delta.ingestion.service;

import com.delta.ingestion.BaseIntegrationTest;
import com.delta.ingestion.dto.IngestResponseDTO;
import com.delta.ingestion.repository.ProcessedRequestRepository;
import com.delta.ingestion.repository.StagingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionServiceTest extends BaseIntegrationTest {

    @Autowired
    private IngestionService ingestionService;
    @Autowired
    private StagingRepository stagingRepository;
    @Autowired
    private IngestionManager ingestionManager;
    @Autowired
    private  ProcessedRequestRepository processedRequestRepository;
    @Autowired
    private  ObjectMapper objectMapper;

    @Test
    void shouldProcessRealStreamingIngestion() {
        // Arrange: Real JSON matching your DTO (ensure snake_case is handled in DTO)
        String requestId = "REQ_REAL_123";
        String json = """
            [
                {
                    "external_id": "C-101",
                    "name": "Vijay",
                    "email": "vijay@test.com",
                    "country_code": "IN",
                    "status_code": "ACTIVE"
                },
                {
                    "external_id": "C-102",
                    "name": "Gemini",
                    "email": "gemini@test.com",
                    "country_code": "US",
                    "status_code": "ACTIVE"
                }
            ]
            """;
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        // Act: Calling the actual service method
        IngestResponseDTO response = ingestionService.ingest(requestId, inputStream, false);

        // Assert: Verify the response matches what the DB actually processed
        assertThat(response).isNotNull();
        assertThat(response.getReceived()).isEqualTo(2);
        assertThat(response.getInserted()).isEqualTo(2);
        assertThat(response.getFailed()).isEqualTo(0);
    }

    @Test
    void shouldHandleIdempotency() {
        String requestId = "REQ_DUPE_999";
        String json = "[{\"external_id\": \"C-200\", \"name\": \"User\"}]";

        // First call
        ingestionService.ingest(requestId, new ByteArrayInputStream(json.getBytes()), false);

        // Second call with same RequestId
        IngestResponseDTO secondResponse = ingestionService.ingest(requestId,
                new ByteArrayInputStream(json.getBytes()), false);

        // Should return existing result instead of reprocessing
        assertThat(secondResponse.getReceived()).isEqualTo(1);
    }
}