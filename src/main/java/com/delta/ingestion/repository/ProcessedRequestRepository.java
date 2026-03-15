package com.delta.ingestion.repository;

import com.delta.ingestion.entity.ProcessedRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProcessedRequestRepository extends JpaRepository<ProcessedRequest, Long> {
    Optional<ProcessedRequest> findByRequestId(String requestId);
}