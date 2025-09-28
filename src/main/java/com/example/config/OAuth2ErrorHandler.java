package com.example.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for OAuth2-related errors.
 * Provides standardized error responses according to OAuth2 specification.
 */
@ControllerAdvice
public class OAuth2ErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleOAuth2Exception(OAuth2AuthenticationException ex) {
        OAuth2Error error = ex.getError();
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error.getErrorCode());
        errorResponse.put("error_description", error.getDescription());
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        
        // Add error URI if available
        if (error.getUri() != null) {
            errorResponse.put("error_uri", error.getUri());
        }
        
        HttpStatus status = determineHttpStatus(error.getErrorCode());
        
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", OAuth2ErrorCodes.INVALID_REQUEST);
        errorResponse.put("error_description", "Invalid request parameter: " + ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", OAuth2ErrorCodes.SERVER_ERROR);
        errorResponse.put("error_description", "An unexpected error occurred");
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        
        // Include detailed error message in development (be careful in production)
        if (isDevelopmentMode()) {
            errorResponse.put("debug_message", ex.getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    /**
     * Determine the appropriate HTTP status code based on OAuth2 error code.
     */
    private HttpStatus determineHttpStatus(String errorCode) {
        return switch (errorCode) {
            case OAuth2ErrorCodes.INVALID_CLIENT -> HttpStatus.UNAUTHORIZED;
            case OAuth2ErrorCodes.INVALID_GRANT, 
                 OAuth2ErrorCodes.INVALID_REQUEST, 
                 OAuth2ErrorCodes.INVALID_SCOPE,
                 OAuth2ErrorCodes.UNSUPPORTED_GRANT_TYPE,
                 OAuth2ErrorCodes.UNSUPPORTED_RESPONSE_TYPE -> HttpStatus.BAD_REQUEST;
            case OAuth2ErrorCodes.ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case OAuth2ErrorCodes.SERVER_ERROR, 
                 OAuth2ErrorCodes.TEMPORARILY_UNAVAILABLE -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    /**
     * Check if application is running in development mode.
     * You can customize this logic based on your profile configuration.
     */
    private boolean isDevelopmentMode() {
        // Simple check - in production, you might want to use Spring profiles
        return !System.getProperty("spring.profiles.active", "").contains("prod");
    }
}