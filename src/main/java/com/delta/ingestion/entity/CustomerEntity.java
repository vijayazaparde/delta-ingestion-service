package com.delta.ingestion.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private CountryEntity country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private CustomerStatusEntity status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== SETTERS =====

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCountry(CountryEntity country) {
        this.country = country;
    }

    public void setStatus(CustomerStatusEntity status) {
        this.status = status;
    }

    // ===== GETTERS =====

    public Long getCustomerId() {
        return customerId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public CountryEntity getCountry() {
        return country;
    }

    public CustomerStatusEntity getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
