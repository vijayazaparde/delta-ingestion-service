package com.delta.ingestion.exception;

import com.delta.ingestion.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponseDTO buildError(
            ErrorCode code,
            String message,
            HttpServletRequest request) {

        return ErrorResponseDTO.builder()
                .errorCode(code)
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
    }

    @ExceptionHandler(InvalidPayloadException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidPayload(
            InvalidPayloadException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildError(
                        ErrorCode.INVALID_PAYLOAD,
                        ex.getMessage(),
                        request));
    }


    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseDTO> handleJdbcException(
            DataAccessException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(
                        ErrorCode.DATABASE_ERROR,
                        "Database error occurred",
                        request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(
                        ErrorCode.INTERNAL_ERROR,
                        "Unexpected server error",
                        request));
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseException(DatabaseException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", java.time.LocalDateTime.now().toString());
        body.put("status", 500);
        body.put("error", "Database Error");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(com.fasterxml.jackson.core.JacksonException.class)
    public ResponseEntity<ErrorResponseDTO> handleJacksonException(
            com.fasterxml.jackson.core.JacksonException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildError(
                        ErrorCode.INVALID_PAYLOAD,
                        "Malformed JSON payload",
                        request));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMediaTypeNotSupported(
            org.springframework.web.HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(buildError(
                        ErrorCode.INVALID_PAYLOAD, // Or define a new ErrorCode.UNSUPPORTED_MEDIA_TYPE
                        "Content-Type '" + ex.getContentType() + "' is not supported",
                        request));
    }
}