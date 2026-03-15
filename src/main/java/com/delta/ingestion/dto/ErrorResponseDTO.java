package com.delta.ingestion.dto;

import com.delta.ingestion.exception.ErrorCode;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ErrorResponseDTO {

    private ErrorCode errorCode;

    private String message;

    private Instant timestamp;

    private String path;

}