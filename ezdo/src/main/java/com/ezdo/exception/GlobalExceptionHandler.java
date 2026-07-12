package com.ezdo.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<Map<String, Object>> handleEcommerceException(ApplicationException e) {
        return buildErrorResponse(e.getMessage(), e.getStatusCode(), e.getErrorCode(), e.getInfo());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
        String message,
        int status,
        String errorCode,
        Map<String, Object> info
    ) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("message", message);
        errorResponse.put("statusCode", status);
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("info", info);
        errorResponse.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(errorResponse);
    }
}
