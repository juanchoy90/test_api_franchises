package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.ResourceNotFoundException;
import com.juandavidg.franchise.domain.model.Store;
import com.juandavidg.franchise.domain.model.StoreStatus;
import com.juandavidg.franchise.domain.model.command.CreateStoreCommand;
import com.juandavidg.franchise.domain.port.in.CreateStoreUseCase;
import com.juandavidg.franchise.domain.port.out.FranchiseRepositoryPort;
import com.juandavidg.franchise.domain.port.out.StoreRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateStoreService implements CreateStoreUseCase {

    private final StoreRepositoryPort storeRepositoryPort;
    private final FranchiseRepositoryPort franchiseRepositoryPort;

    @Override
    public Mono<Store> execute(CreateStoreCommand command) {
        log.info("Checking franchise existence for franchiseId={}", command.franchiseId());

        return franchiseRepositoryPort.existsById(command.franchiseId())
                .flatMap(exists -> {
                    if (Boolean.FALSE.equals(exists)) {
                        log.warn("Store creation rejected, franchiseId={} does not exist", command.franchiseId());
                        return Mono.error(new ResourceNotFoundException("Franchise", "franchiseId", command.franchiseId()));
                    }
                    Instant now = Instant.now();
                    Store store = Store.builder()
                            .id(UUID.randomUUID().toString())
                            .franchiseId(command.franchiseId())
                            .name(command.name())
                            .address(command.address())
                            .city(command.city())
                            .phone(command.phone())
                            .status(StoreStatus.ACTIVE)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    log.debug("Persisting new store id={} franchiseId={}", store.getId(), store.getFranchiseId());
                    return storeRepositoryPort.save(store);
                })
                .doOnNext(store -> log.info("Store created successfully id={} franchiseId={}", store.getId(), store.getFranchiseId()));
    }
}
