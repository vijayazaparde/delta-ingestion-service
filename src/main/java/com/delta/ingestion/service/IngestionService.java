package com.delta.ingestion.service;

import com.delta.ingestion.dto.IncomingCustomerDTO;
import com.delta.ingestion.dto.IngestResponseDTO;
import com.delta.ingestion.entity.ProcessedRequest;
import com.delta.ingestion.exception.DatabaseException;
import com.delta.ingestion.exception.InvalidPayloadException;
import com.delta.ingestion.repository.ProcessedRequestRepository;
import com.delta.ingestion.repository.StagingRepository;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final IngestionManager ingestionManager;
    private final StagingRepository stagingRepository;
    private final ProcessedRequestRepository processedRequestRepository;
    private final ObjectMapper objectMapper;

    private final Counter ingestionRecordsCounter;
    private final Counter ingestionFailureCounter;

    private static final int BATCH_SIZE = 1000;

    public IngestResponseDTO ingest(String requestId, InputStream is, boolean dryRun) {

        long startTime = System.currentTimeMillis();

        ingestionManager.preparePartition(LocalDate.now());

        try {
            ingestionManager.createPendingLock(requestId);
        } catch (DatabaseException e) {
            log.warn("Duplicate request detected for ID: {}", requestId);
            return handleDuplicateRequest(requestId);
        }

        int totalReceived = 0;
        int invalidRecords = 0;

        OffsetDateTime requestCreatedAt = OffsetDateTime.now();

        List<IncomingCustomerDTO> batch = new ArrayList<>(BATCH_SIZE);

        try (JsonParser parser = objectMapper.getFactory().createParser(is)) {

            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new InvalidPayloadException("Payload must be a JSON array");
            }

            ObjectReader reader = objectMapper.readerFor(IncomingCustomerDTO.class);

            while (parser.nextToken() != JsonToken.END_ARRAY) {

                if (parser.currentToken() == JsonToken.START_OBJECT) {

                    IncomingCustomerDTO customer = reader.readValue(parser);
                    totalReceived++;

                    String extId = customer.getExternalId();

                    if (extId == null || extId.isBlank()) {
                        invalidRecords++;
                        continue;
                    }

                    customer.setExternalId(extId.trim());
                    batch.add(customer);

                    if (batch.size() >= BATCH_SIZE) {

                        stagingRepository.copyInsert(
                                requestId,
                                requestCreatedAt,
                                batch
                        );

                        batch.clear();
                    }
                }
            }

            if (!batch.isEmpty()) {
                stagingRepository.copyInsert(
                        requestId,
                        requestCreatedAt,
                        batch
                );
            }

            ingestionRecordsCounter.increment(totalReceived);

            ProcessedRequest pr = ingestionManager.finalizeIngestion(
                    requestId,
                    totalReceived,
                    dryRun,
                    0,
                    invalidRecords
            );

            long duration = System.currentTimeMillis() - startTime;

            return mapToDTO(pr, duration);

        } catch (JsonProcessingException e) {

            ingestionFailureCounter.increment();
            ingestionManager.rollbackLock(requestId);

            throw new InvalidPayloadException("Malformed JSON: " + e.getMessage());

        } catch (Exception e) {

            ingestionFailureCounter.increment();
            ingestionManager.rollbackLock(requestId);

            log.error("Ingestion failed for request {}", requestId, e);

            throw new DatabaseException("Failed to process ingestion", e);
        }
    }

    private IngestResponseDTO handleDuplicateRequest(String requestId) {

        return processedRequestRepository.findByRequestId(requestId)
                .map(pr -> mapToDTO(pr, 0))
                .orElseThrow(() ->
                        new DatabaseException(
                                "Ingestion for request " + requestId + " is currently in progress."
                        ));
    }

    private IngestResponseDTO mapToDTO(ProcessedRequest pr, long durationMs) {

        double hitRatio = pr.getReceived() == 0
                ? 0.0
                : (double) pr.getSkipped() / pr.getReceived();

        return IngestResponseDTO.builder()
                .received(pr.getReceived())
                .inserted(pr.getInserted())
                .skippedExisting(pr.getSkipped())
                .failed(pr.getFailed())
                .timeTaken(durationMs + "ms")
                .rowsScanned(pr.getReceived())
                .cacheHitRatio(Math.round(hitRatio * 100.0) / 100.0)
                .build();
    }
}