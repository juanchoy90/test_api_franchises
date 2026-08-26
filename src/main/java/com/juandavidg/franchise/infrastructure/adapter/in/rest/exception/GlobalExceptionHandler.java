package com.juandavidg.franchise.infrastructure.adapter.in.rest.exception;

import com.juandavidg.franchise.domain.exception.DuplicateResourceException;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.FranchiseController;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ErrorDetail;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex) {
        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "VALIDATION_ERROR", "Request validation failed", Instant.now(), errors));
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidIdempotencyKey(InvalidIdempotencyKeyException ex) {
        List<ErrorDetail> errors = List.of(new ErrorDetail(FranchiseController.IDEMPOTENCY_HEADER, ex.getMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "VALIDATION_ERROR", ex.getMessage(), Instant.now(), errors));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        List<ErrorDetail> errors = List.of(new ErrorDetail(ex.getField(), ex.getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                409, "DUPLICATE_RESOURCE", ex.getMessage(), Instant.now(), errors));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleMissingInput(ServerWebInputException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "MISSING_INPUT", ex.getReason(), Instant.now(), List.of()));
    }
}
