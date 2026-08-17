package com.ezdo.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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

    // Covers @Validated on @RequestParam / @PathVariable (bean validation outside the request body)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException e) {
        List<Map<String, Object>> errors = e.getConstraintViolations().stream()
            .map(cv -> {
                Map<String, Object> m = new HashMap<>();
                m.put("field", cv.getPropertyPath().toString());
                m.put("message", cv.getMessage());
                m.put("rejectedValue", cv.getInvalidValue());
                return m;
            })
            .toList();

        return buildErrorResponse(
            "Validation failed",
            HttpStatus.UNPROCESSABLE_CONTENT.value(),
            ErrorCodes.VALIDATION_ERROR,
            Map.of("errors", errors)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedBody(HttpMessageNotReadableException e) {
        return buildErrorResponse(
            "Malformed or missing request body",
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.MALFORMED_REQUEST_BODY,
            Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("parameter", e.getName());
        info.put("rejectedValue", e.getValue());

        Class<?> required = e.getRequiredType();
        if (required != null && required.isEnum()) {
            info.put("allowedValues", Arrays.stream(required.getEnumConstants())
                .map(Object::toString)
                .toList());
        }

        return buildErrorResponse(
            "Invalid value for '" + e.getName() + "'",
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.TYPE_MISMATCH,
            info
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return buildErrorResponse(
            e.getMessage(),
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            ErrorCodes.METHOD_NOT_ALLOWED,
            Map.of()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        return buildErrorResponse(
            "You do not have permission to perform this action",
            HttpStatus.FORBIDDEN.value(),
            ErrorCodes.ACCESS_DENIED,
            Map.of()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException e) {
        return buildErrorResponse(
            "Authentication failed",
            HttpStatus.UNAUTHORIZED.value(),
            ErrorCodes.AUTHENTICATION_FAILED,
            Map.of()
        );
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockingFailure(
        OptimisticLockingFailureException e
    ) {
        log.warn("Concurrent modification of a versioned row", e);
        return buildErrorResponse(
            "This was modified by another request at the same time. Reload and try again",
            HttpStatus.CONFLICT.value(),
            ErrorCodes.CONCURRENT_MODIFICATION,
            Map.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return buildErrorResponse(
            "The request could not be completed due to a data conflict",
            HttpStatus.CONFLICT.value(),
            ErrorCodes.DATA_INTEGRITY_VIOLATION,
            Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unhandled exception", e);
        return buildErrorResponse(
            "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ErrorCodes.INTERNAL_SERVER_ERROR,
            Map.of()
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
