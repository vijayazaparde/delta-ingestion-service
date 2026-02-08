package com.delta.ingestion.repository;

import com.delta.ingestion.entity.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<CountryEntity, Long> {

    Optional<CountryEntity> findByCodeIgnoreCase(String code);
}
