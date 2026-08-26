package com.juandavidg.franchise.domain.port.out;

import com.juandavidg.franchise.domain.model.Franchise;
import reactor.core.publisher.Mono;

public interface FranchiseRepositoryPort {

    Mono<Franchise> save(Franchise franchise);

    Mono<Boolean> existsByNit(String nit);

    Mono<Boolean> existsById(String id);
}
