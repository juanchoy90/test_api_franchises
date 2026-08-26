package com.juandavidg.franchise.infrastructure.adapter.in.rest.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key must be a valid UUIDv4");
    }
}
