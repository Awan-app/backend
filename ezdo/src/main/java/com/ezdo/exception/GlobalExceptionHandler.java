package com.ezdo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationException(ApplicationException e) {
        return buildErrorResponse(e.getMessage(), e.getStatusCode(), e.getErrorCode(), e.getInfo());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        List<Map<String, Object>> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("field", fe.getField());
                    errorMap.put("message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value");
                    errorMap.put("rejectedValue", fe.getRejectedValue());
                    return errorMap;
                })
                .toList();

        return buildErrorResponse(
                "Validation failed",
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                "VALIDATION_ERROR",
                Map.of("errors", fieldErrors)
        );
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
