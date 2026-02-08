package com.delta.ingestion.controller;

import com.delta.ingestion.dto.IncomingCustomerDTO;
import com.delta.ingestion.dto.IngestResponseDTO;
import com.delta.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponseDTO> ingestCustomers(
            @RequestBody @Valid List<IncomingCustomerDTO> payload
    ) {
        IngestResponseDTO response = ingestionService.ingest(payload);
        return ResponseEntity.ok(response);
    }
}
