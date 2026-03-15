package com.delta.ingestion.exception;

import com.delta.ingestion.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleInvalidPayload() {
        // Arrange
        InvalidPayloadException ex = new InvalidPayloadException("Invalid data");
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/ingest");

        // Act
        ResponseEntity<ErrorResponseDTO> response = handler.handleInvalidPayload(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid data");
        assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYLOAD);
    }
}