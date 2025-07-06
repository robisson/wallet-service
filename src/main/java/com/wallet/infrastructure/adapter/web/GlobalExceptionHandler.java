package com.wallet.infrastructure.adapter.web;

import com.wallet.infrastructure.metrics.WalletMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the wallet service REST API.
 * 
 * <p>This class provides centralized exception handling for all REST endpoints,
 * ensuring consistent error responses and proper logging. It handles both
 * business logic exceptions and validation errors.
 * 
 * <p>Exception handling includes:
 * <ul>
 *   <li>Business rule violations (IllegalArgumentException)</li>
 *   <li>Request validation failures (MethodArgumentNotValidException)</li>
 *   <li>Unexpected system errors (Exception)</li>
 * </ul>
 * 
 * <p>All exceptions are logged with appropriate levels and metrics are
 * collected for monitoring and alerting purposes.
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final WalletMetrics walletMetrics;
    
    /**
     * Constructs a new GlobalExceptionHandler with metrics support.
     * 
     * @param walletMetrics the metrics service for error tracking
     */
    public GlobalExceptionHandler(WalletMetrics walletMetrics) {
        this.walletMetrics = walletMetrics;
    }
    
    /**
     * Handles business rule violations and invalid argument exceptions.
     * 
     * <p>These exceptions typically occur when business rules are violated,
     * such as insufficient funds, invalid amounts, or wallet not found.
     * 
     * @param ex the illegal argument exception
     * @return 400 Bad Request with error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        MDC.put("errorType", "IllegalArgumentException");
        logger.warn("Business rule violation: {}", ex.getMessage());
        walletMetrics.incrementBusinessError("illegal_argument");
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Handles request validation failures.
     * 
     * <p>These exceptions occur when request data fails validation,
     * such as missing required fields, invalid formats, or constraint violations.
     * 
     * @param ex the validation exception
     * @return 400 Bad Request with field-specific error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        MDC.put("errorType", "ValidationException");
        logger.warn("Validation failed for request");
        walletMetrics.incrementBusinessError("validation_failed");
        
        Map<String, Object> error = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        
        error.put("error", "Validation Failed");
        error.put("fieldErrors", fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Handles unexpected system errors.
     * 
     * <p>This is the catch-all handler for any exceptions not specifically
     * handled by other methods. It logs the full exception details and
     * returns a generic error message to avoid exposing internal details.
     * 
     * @param ex the unexpected exception
     * @return 500 Internal Server Error with generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        MDC.put("errorType", ex.getClass().getSimpleName());
        logger.error("Unexpected error occurred", ex);
        walletMetrics.incrementBusinessError("unexpected_error");
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal Server Error");
        error.put("message", "An unexpected error occurred");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}