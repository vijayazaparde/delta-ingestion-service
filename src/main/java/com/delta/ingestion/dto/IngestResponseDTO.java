package com.delta.ingestion.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IngestResponseDTO {

    private int received;
    private int inserted;
    private int skippedExisting;
    private int failed;
    private String timeTaken;
    private long rowsScanned;
    private double cacheHitRatio;
}
