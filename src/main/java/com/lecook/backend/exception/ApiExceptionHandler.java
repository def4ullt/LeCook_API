package com.lecook.backend.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(String code, String message, Map<String, String> fields) {}

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> missing(ResourceNotFoundException error) {
        return ResponseEntity.status(404).body(new ApiError("not_found", error.getMessage(), Map.of()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("validation_failed", "Перевірте введені дані", fields));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        HandlerMethodValidationException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> malformed(Exception error) {
        return ResponseEntity.badRequest().body(new ApiError("invalid_request", "Некоректний запит або параметри", Map.of()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException error) {
        return ResponseEntity.status(409).body(new ApiError("data_conflict", "Запис використовується іншими даними або порушує обмеження бази", Map.of()));
    }

    @ExceptionHandler(ResourceConflictException.class)
    ResponseEntity<ApiError> duplicate(ResourceConflictException error) {
        return ResponseEntity.status(409).body(new ApiError("data_conflict", error.getMessage(), Map.of()));
    }
}
