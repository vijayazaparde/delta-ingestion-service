package com.delta.ingestion.service;

import com.delta.ingestion.dto.IncomingCustomerDTO;
import com.delta.ingestion.dto.IngestResponseDTO;
import com.delta.ingestion.repository.CustomerRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
public class IngestionServiceTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private CustomerRepository customerRepo;

    @Test
    void shouldInsertOnlyDelta() {

        List<IncomingCustomerDTO> batch = List.of(
                new IncomingCustomerDTO("cust_1","A","a@test.com","US","ACTIVE"),
                new IncomingCustomerDTO("cust_2","B","b@test.com","IN","ACTIVE")
        );

        IngestResponseDTO response1 = ingestionService.ingest(batch);

        assertEquals(2, response1.getInserted());
        assertEquals(0, response1.getSkipped());

        // Re-run same batch (idempotency check)
        IngestResponseDTO response2 = ingestionService.ingest(batch);

        assertEquals(0, response2.getInserted());
        assertEquals(2, response2.getSkipped());
    }
}