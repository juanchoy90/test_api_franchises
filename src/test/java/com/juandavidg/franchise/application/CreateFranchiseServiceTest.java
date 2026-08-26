package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.DuplicateResourceException;
import com.juandavidg.franchise.domain.model.Franchise;
import com.juandavidg.franchise.domain.model.FranchiseStatus;
import com.juandavidg.franchise.domain.model.command.CreateFranchiseCommand;
import com.juandavidg.franchise.domain.port.out.FranchiseRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFranchiseServiceTest {

    @Mock
    private FranchiseRepositoryPort franchiseRepositoryPort;

    @InjectMocks
    private CreateFranchiseService createFranchiseService;

    private static final CreateFranchiseCommand COMMAND = new CreateFranchiseCommand(
            "Franquicia Central", "900.123.456-7",
            "Bogotá", "Colombia", "contacto@franquiciacentral.com"
    );

    @Test
    @DisplayName("execute: debe guardar y retornar la franquicia cuando el NIT no existe")
    void execute_shouldSaveAndReturnFranchise_whenNitDoesNotExist() {
        Franchise saved = Franchise.builder()
                .id(UUID.randomUUID().toString())
                .name(COMMAND.name()).nit(COMMAND.nit())
                .city(COMMAND.city()).country(COMMAND.country()).email(COMMAND.email())
                .status(FranchiseStatus.ACTIVE)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(franchiseRepositoryPort.existsByNit(COMMAND.nit())).thenReturn(Mono.just(false));
        when(franchiseRepositoryPort.save(any(Franchise.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(createFranchiseService.execute(COMMAND))
                .assertNext(franchise -> {
                    assertThat(franchise.getNit()).isEqualTo(COMMAND.nit());
                    assertThat(franchise.getStatus()).isEqualTo(FranchiseStatus.ACTIVE);
                    assertThat(franchise.getId()).isNotBlank();
                    assertThat(franchise.getCreatedAt()).isNotNull();
                })
                .verifyComplete();

        verify(franchiseRepositoryPort).existsByNit(COMMAND.nit());
        verify(franchiseRepositoryPort).save(any(Franchise.class));
    }

    @Test
    @DisplayName("execute: debe retornar DuplicateResourceException cuando el NIT ya existe")
    void execute_shouldReturnDuplicateResourceException_whenNitAlreadyExists() {
        when(franchiseRepositoryPort.existsByNit(COMMAND.nit())).thenReturn(Mono.just(true));

        StepVerifier.create(createFranchiseService.execute(COMMAND))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DuplicateResourceException.class);
                    DuplicateResourceException ex = (DuplicateResourceException) error;
                    assertThat(ex.getField()).isEqualTo("nit");
                    assertThat(ex.getValue()).isEqualTo(COMMAND.nit());
                })
                .verify();

        verify(franchiseRepositoryPort).existsByNit(COMMAND.nit());
        verify(franchiseRepositoryPort, never()).save(any());
    }
}
