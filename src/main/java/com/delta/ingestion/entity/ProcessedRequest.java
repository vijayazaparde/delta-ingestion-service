package com.delta.ingestion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "processed_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String requestId;

    // Use a String or Enum for status
    @Builder.Default
    private String status = "PENDING";

    private Integer received;
    private Integer inserted;
    private Integer skipped;
    private Integer failed;

    private Instant startedAt;
    private Instant processedAt;
}