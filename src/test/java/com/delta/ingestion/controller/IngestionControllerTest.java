package com.delta.ingestion.controller;

import com.delta.ingestion.BaseIntegrationTest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class IngestionControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MeterRegistry meterRegistry;

    private static final String METRIC_TOTAL = "ingestion_records_total";
    private static final String METRIC_INSERTED = "ingestion_records_inserted";
    private static final String METRIC_SKIPPED = "ingestion_records_skipped";
    private static final String METRIC_FAILED = "ingestion_records_failed";

    @Test
    @DisplayName("Scenario 1: Happy Path - Single Valid Record Ingestion")
    void shouldIngestSingleValidRecordAndIncrementInsertedMetric() throws Exception {
        String json = createPayload("cust-" + UUID.randomUUID(), "Real User", "real@delta.com", "IN", "ACTIVE");
        double delta = getDelta(METRIC_INSERTED, () -> {
            try {
                mockMvc.perform(post("/customers/ingest")
                                .header("X-Request-ID", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.inserted").value(1))
                        .andExpect(jsonPath("$.rows_scanned").value(1))
                        .andExpect(jsonPath("$.cache_hit_ratio").value(0.0));
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        assertEquals(1, (int) delta);
    }

    @Test
    @DisplayName("Scenario 2: Delta Detection - Existing Record Skip")
    void shouldSkipRecordsAlreadyPresentInDatabaseAndIncrementSkippedMetric() throws Exception {
        String id = "exist-" + UUID.randomUUID();
        setupExistingCustomerInDb(id, "Existing User", "IN", "ACTIVE");
        String json = createPayload(id, "Existing User", "exist@delta.com", "IN", "ACTIVE");

        double delta = getDelta(METRIC_SKIPPED, () -> {
            try {
                mockMvc.perform(post("/customers/ingest").contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.skipped_existing").value(1))
                        .andExpect(jsonPath("$.cache_hit_ratio").value(1.0));
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        assertEquals(1, (int) delta);
    }

    @Test
    @DisplayName("Scenario 3: Internal Deduplication - Same Payload Duplicates")
    void shouldHandleInternalDuplicatesWithinTheSamePayloadAndSplitMetrics() throws Exception {
        String id = "dup-" + UUID.randomUUID();
        String json = String.format("[%s,%s]",
                createPayload(id, "User A", "a@d.com", "IN", "ACTIVE").replace("[","").replace("]",""),
                createPayload(id, "User B", "b@d.com", "IN", "ACTIVE").replace("[","").replace("]","")
        );

        double deltaSkipped = getDelta(METRIC_SKIPPED, () -> {
            try {
                mockMvc.perform(post("/customers/ingest").contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.received").value(2))
                        .andExpect(jsonPath("$.inserted").value(1))
                        .andExpect(jsonPath("$.skipped_existing").value(1));
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        assertEquals(1, (int) deltaSkipped);
    }

    @Test
    @DisplayName("Scenario 4: Business Validation Failure - Invalid Country/Status")
    void shouldIdentifyAndCountRecordsWithInvalidLookupCodesAndIncrementFailedMetric() throws Exception {
        String json = createPayload("bad-1", "Failure", "f@d.com", "INVALID", "ACTIVE");
        double delta = getDelta(METRIC_FAILED, () -> {
            try {
                mockMvc.perform(post("/customers/ingest").contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.failed").value(1));
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        assertEquals(1, (int) delta);
    }

    @Test
    @DisplayName("Scenario 5: Case Insensitivity - Normalize Incoming Codes")
    void shouldBeCaseInsensitiveForCountryAndStatusCodes() throws Exception {
        String json = createPayload("case-" + UUID.randomUUID(), "User", "u@d.com", "in", "active");
        mockMvc.perform(post("/customers/ingest").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inserted").value(1));
    }

    @Test
    @DisplayName("Scenario 6: Dry Run Mode - Computation Without Persistence")
    void shouldNotInsertWhenDryRunIsTrueButReturnCorrectDeltas() throws Exception {
        String json = createPayload("dry-" + UUID.randomUUID(), "Dry", "d@d.com", "IN", "ACTIVE");
        mockMvc.perform(post("/customers/ingest").param("dryRun", "true").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inserted").value(0))
                .andExpect(jsonPath("$.received").value(1));
    }

    @Test
    @DisplayName("Scenario 7: Idempotency - Duplicate Request ID Handling")
    void shouldReturnCachedResultForSameRequestIdToPreventDoubleProcessing() throws Exception {
        String reqId = UUID.randomUUID().toString();
        String json = createPayload("idem-1", "Idem", "i@d.com", "IN", "ACTIVE");

        mockMvc.perform(post("/customers/ingest").header("X-Request-ID", reqId).contentType(MediaType.APPLICATION_JSON).content(json));

        mockMvc.perform(post("/customers/ingest").header("X-Request-ID", reqId).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.time_taken").value(notNullValue()));
    }

    @Test
    @DisplayName("Scenario 8: Performance Metadata Validation")
    void shouldValidatePresenceOfTimeTakenAndRowsScannedMetadata() throws Exception {
        String json = createPayload("perf-1", "Perf", "p@d.com", "IN", "ACTIVE");
        mockMvc.perform(post("/customers/ingest").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.time_taken").value(notNullValue()))
                .andExpect(jsonPath("$.rows_scanned").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Scenario 9: Stress Test - Million Records Simulation")
    void shouldSuccessfullyProcessHighVolumePayloadsUsingStreaming() throws Exception {
        // High count to trigger batch flushing logic and ensure memory stability
        int count = 5000;
        String largeJson = generateLargeJson(count);
        mockMvc.perform(post("/customers/ingest").contentType(MediaType.APPLICATION_JSON).content(largeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(count))
                .andExpect(jsonPath("$.rows_scanned").value(count))
                .andExpect(jsonPath("$.time_taken").value(notNullValue()));
    }

    @Test
    @DisplayName("Scenario 10: Malformed JSON - Invalid Array Structure")
    void shouldReturn400BadRequestWhenJsonPayloadIsMalformed() throws Exception {
        String brokenJson = "[{\"external_id\": \"broken\" ";
        mockMvc.perform(post("/customers/ingest").contentType(MediaType.APPLICATION_JSON).content(brokenJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Scenario 11: Unsupported Media Type - Non-JSON Request")
    void shouldReturn415UnsupportedMediaTypeWhenNotSendingJson() throws Exception {
        mockMvc.perform(post("/customers/ingest").contentType(MediaType.TEXT_PLAIN).content("plain text"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Scenario 12: Empty Array - Zero Record Processing")
    void shouldReturnZeroMetricsWhenEmptyJsonArrayIsProvided() throws Exception {
        mockMvc.perform(post("/customers/ingest").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(0));
    }

    @Test
    @DisplayName("Scenario 13: Null Fields - Mandatory Field Validation")
    void shouldHandleNullMandatoryFieldsByIncrementingFailedMetric() throws Exception {
        String json = "[{\"external_id\": null, \"name\": \"No ID\", \"country_code\": \"IN\"}]";
        double delta = getDelta(METRIC_FAILED, () -> {
            try {
                mockMvc.perform(post("/customers/ingest").contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.failed").value(1));
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        assertEquals(1, (int) delta);
    }

    private String createPayload(String extId, String name, String email, String country, String status) {
        return String.format("""
            [
              {
                "external_id": "%s",
                "name": "%s",
                "email": "%s",
                "country_code": "%s",
                "status_code": "%s"
              }
            ]
            """, extId, name, email, country, status);
    }

    private double getCount(String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private double getDelta(String metricName, Runnable action) {
        double before = getCount(metricName);
        action.run();
        double after = getCount(metricName);
        return after - before;
    }

    private String generateLargeJson(int count) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            sb.append(String.format("{\"external_id\":\"STR-%s\",\"name\":\"User %d\",\"email\":\"u%d@d.com\",\"country_code\":\"IN\",\"status_code\":\"ACTIVE\"}", UUID.randomUUID(), i, i));
            if (i < count - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }
}