package com.juandavidg.franchise.infrastructure.adapter.in.rest.exception;

import com.juandavidg.franchise.domain.exception.DuplicateResourceException;
import com.juandavidg.franchise.domain.exception.ProductNotFoundException;
import com.juandavidg.franchise.domain.exception.ResourceNotFoundException;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.FranchiseController;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ErrorDetail;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex) {
        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("Request validation failed with {} error(s)", errors.size());
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "VALIDATION_ERROR", "Request validation failed", Instant.now(), errors));
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidIdempotencyKey(InvalidIdempotencyKeyException ex) {
        List<ErrorDetail> errors = List.of(new ErrorDetail(FranchiseController.IDEMPOTENCY_HEADER, ex.getMessage()));
        log.warn("Invalid Idempotency-Key header: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "VALIDATION_ERROR", ex.getMessage(), Instant.now(), errors));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        List<ErrorDetail> errors = List.of(new ErrorDetail(ex.getField(), ex.getMessage()));
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                409, "DUPLICATE_RESOURCE", ex.getMessage(), Instant.now(), errors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        List<ErrorDetail> errors = List.of(new ErrorDetail(ex.getField(), ex.getMessage()));
        log.warn("Referenced resource not found: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "REFERENCED_RESOURCE_NOT_FOUND", ex.getMessage(), Instant.now(), errors));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        List<ErrorDetail> errors = List.of(new ErrorDetail("productId", ex.getMessage()));
        log.warn("Product not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                404, "PRODUCT_NOT_FOUND", ex.getMessage(), Instant.now(), errors));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleMissingInput(ServerWebInputException ex) {
        log.warn("Missing or malformed request input: {}", ex.getReason());
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "MISSING_INPUT", ex.getReason(), Instant.now(), List.of()));
    }
}
