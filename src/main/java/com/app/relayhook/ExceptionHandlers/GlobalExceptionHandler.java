package com.app.relayhook.ExceptionHandlers;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", java.time.LocalDateTime.now());
        error.put("status", 400);
        error.put("error", "Validation failed");
        error.put("fieldErrors", fieldErrors);
        // trace at the bottom
        error.put("trace", getStackTrace(ex));

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", java.time.LocalDateTime.now());
        error.put("status", ex.getStatusCode().value());
        error.put("error", ex.getReason());
        // trace at the bottom
        error.put("trace", getStackTrace(ex));
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", java.time.LocalDateTime.now());
        error.put("status", 500);
        error.put("error", "Internal Server Error");
        error.put("message", ex.getMessage());
        // trace at the bottom
        error.put("trace", getStackTrace(ex));
        return ResponseEntity.internalServerError().body(error);
    }

    private String getStackTrace(Exception ex) {
        return java.util.Arrays.stream(ex.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));
    }
}
