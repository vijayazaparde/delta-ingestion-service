package com.delta.ingestion.controller;

import com.delta.ingestion.dto.IngestResponseDTO;
import com.delta.ingestion.service.IngestionService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;
    private final Bucket bucket;

    @PostMapping(value = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IngestResponseDTO> ingestCustomers(
            @RequestHeader(value = "requestId", required = false) String requestId,
            @RequestParam(defaultValue = "false") boolean dryRun,
            HttpServletRequest request
    ) throws IOException {

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        final String finalRequestId = (requestId == null || requestId.isBlank())
                ? UUID.randomUUID().toString()
                : requestId;

        log.info("Starting ingestion. RequestId: {}, DryRun: {}", finalRequestId, dryRun);

        IngestResponseDTO response = ingestionService.ingest(
                finalRequestId,
                request.getInputStream(),
                dryRun
        );

        return ResponseEntity.ok(response);
    }
}