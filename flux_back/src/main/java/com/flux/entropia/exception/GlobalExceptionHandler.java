package com.flux.entropia.exception;

import com.flux.entropia.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the application.
 * Catches unhandled exceptions and formats them into a standard ApiResponse.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all generic exceptions.
     * @param ex The exception that was thrown.
     * @return A ResponseEntity containing a standard error ApiResponse.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex) {
        log.error("An unexpected error occurred: ", ex);
        ApiResponse<Object> errorResponse = ApiResponse.error("An internal server error occurred. Please try again later.");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles exceptions thrown for RESTful services, like rate limiting.
     * @param ex The exception containing the HTTP status and reason.
     * @return A ResponseEntity with the specific status and error message.
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        log.warn("ResponseStatusException caught: Status={}, Reason={}", ex.getStatusCode(), ex.getReason());
        ApiResponse<Object> errorResponse = ApiResponse.error(ex.getReason());
        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }

    // You can add more specific exception handlers here, for example:
    /*
    @ExceptionHandler(YourSpecificBusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(YourSpecificBusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        ApiResponse<Object> errorResponse = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    */
}
