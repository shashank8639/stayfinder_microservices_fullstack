package com.stayfinder.food.exception;

import com.stayfinder.food.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ApiError> handleInvalid(InvalidOrderException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(BookingDependencyException.class)
    public ResponseEntity<ApiError> handleDependency(BookingDependencyException ex, HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Authentication required", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error path={}", request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(),
                status.value(),
                status.name(),
                message,
                request.getRequestURI()
        ));
    }
}
