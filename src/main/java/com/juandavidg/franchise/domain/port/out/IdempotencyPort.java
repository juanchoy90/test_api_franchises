package com.juandavidg.franchise.domain.port.out;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface IdempotencyPort {

    Mono<String> findByKey(String idempotencyKey);

    Mono<Void> save(String idempotencyKey, String responseJson, Duration ttl);
}
