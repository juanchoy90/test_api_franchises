package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.DuplicateResourceException;
import com.juandavidg.franchise.domain.model.Franchise;
import com.juandavidg.franchise.domain.model.FranchiseStatus;
import com.juandavidg.franchise.domain.model.command.CreateFranchiseCommand;
import com.juandavidg.franchise.domain.port.in.CreateFranchiseUseCase;
import com.juandavidg.franchise.domain.port.out.FranchiseRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateFranchiseService implements CreateFranchiseUseCase {

    private final FranchiseRepositoryPort franchiseRepositoryPort;

    @Override
    public Mono<Franchise> execute(CreateFranchiseCommand command) {
        return franchiseRepositoryPort.existsByNit(command.nit())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new DuplicateResourceException("Franchise", "nit", command.nit()));
                    }
                    Instant now = Instant.now();
                    Franchise franchise = Franchise.builder()
                            .id(UUID.randomUUID().toString())
                            .name(command.name())
                            .nit(command.nit())
                            .city(command.city())
                            .country(command.country())
                            .email(command.email())
                            .status(FranchiseStatus.ACTIVE)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return franchiseRepositoryPort.save(franchise);
                });
    }
}
