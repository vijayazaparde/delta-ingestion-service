package com.delta.ingestion.repository;

import com.delta.ingestion.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByExternalId(String externalId);

    @Query("SELECT c.externalId FROM CustomerEntity c WHERE c.externalId IN :externalIds")
    Set<String> findByExternalIdIn(Set<String> externalIds);
}
