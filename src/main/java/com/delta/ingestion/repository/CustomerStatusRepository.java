package com.delta.ingestion.repository;

import com.delta.ingestion.entity.CustomerStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerStatusRepository extends JpaRepository<CustomerStatusEntity, Long> {

    Optional<CustomerStatusEntity> findByCodeIgnoreCase(String code);
}
