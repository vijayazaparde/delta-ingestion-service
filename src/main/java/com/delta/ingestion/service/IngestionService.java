package com.delta.ingestion.service;

import com.delta.ingestion.dto.IncomingCustomerDTO;
import com.delta.ingestion.dto.IngestResponseDTO;
import com.delta.ingestion.entity.CountryEntity;
import com.delta.ingestion.entity.CustomerEntity;
import com.delta.ingestion.entity.CustomerStatusEntity;
import com.delta.ingestion.repository.CountryRepository;
import com.delta.ingestion.repository.CustomerRepository;
import com.delta.ingestion.repository.CustomerStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class IngestionService {

    private final CustomerRepository customerRepo;
    private final CountryRepository countryRepo;
    private final CustomerStatusRepository statusRepo;

    public IngestionService(CustomerRepository customerRepo,
                            CountryRepository countryRepo,
                            CustomerStatusRepository statusRepo) {
        this.customerRepo = customerRepo;
        this.countryRepo = countryRepo;
        this.statusRepo = statusRepo;
    }

    @Transactional
    public IngestResponseDTO ingest(List<IncomingCustomerDTO> incoming) {
        int received = incoming.size();
        int failed = 0;

        // 1. Extract external IDs
        Set<String> externalIds = incoming.stream()
                .map(IncomingCustomerDTO::getExternal_id)
                .collect(Collectors.toSet());

        // 2. Fetch existing IDs (duplicates)
        Set<String> existing = customerRepo.findByExternalIdIn(externalIds);

        // 3. Delta = new customers only
        List<IncomingCustomerDTO> delta = incoming.stream()
                .filter(c -> !existing.contains(c.getExternal_id()))
                .toList();

        List<CustomerEntity> toInsert = new ArrayList<>();

        // 4. Map DTO → Entity
        for (IncomingCustomerDTO dto : delta) {
            try {
                CountryEntity country = countryRepo.findByCodeIgnoreCase(dto.getCountry_code())
                        .orElseThrow(() -> new RuntimeException("Invalid country code: " + dto.getCountry_code()));

                CustomerStatusEntity status = statusRepo.findByCodeIgnoreCase(dto.getStatus_code())
                        .orElseThrow(() -> new RuntimeException("Invalid status code: " + dto.getStatus_code()));

                CustomerEntity c = new CustomerEntity();
                c.setExternalId(dto.getExternal_id());
                c.setName(dto.getName());
                c.setEmail(dto.getEmail());
                c.setCountry(country);
                c.setStatus(status);

                toInsert.add(c);

            } catch (Exception e) {
                failed++;
            }
        }

        // 5. Save
        customerRepo.saveAll(toInsert);

        // 6. Response
        return new IngestResponseDTO(
                received,                     // total received
                toInsert.size(),               // inserted
                received - toInsert.size() - failed, // skipped (duplicates)
                failed                         // failed
        );
    }
}
