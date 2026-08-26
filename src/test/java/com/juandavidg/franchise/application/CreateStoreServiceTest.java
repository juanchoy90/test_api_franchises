package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.ResourceNotFoundException;
import com.juandavidg.franchise.domain.model.Store;
import com.juandavidg.franchise.domain.model.StoreStatus;
import com.juandavidg.franchise.domain.model.command.CreateStoreCommand;
import com.juandavidg.franchise.domain.port.out.FranchiseRepositoryPort;
import com.juandavidg.franchise.domain.port.out.StoreRepositoryPort;
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
class CreateStoreServiceTest {

    @Mock
    private StoreRepositoryPort storeRepositoryPort;

    @Mock
    private FranchiseRepositoryPort franchiseRepositoryPort;

    @InjectMocks
    private CreateStoreService createStoreService;

    private static final CreateStoreCommand COMMAND = new CreateStoreCommand(
            "fran_123", "Sucursal Norte", "Cra 7 # 45-12", "Bogotá", "6011234567"
    );

    @Test
    @DisplayName("execute: debe guardar y retornar la sucursal cuando la franquicia existe")
    void execute_shouldSaveAndReturnStore_whenFranchiseExists() {
        Store saved = Store.builder()
                .id(UUID.randomUUID().toString())
                .franchiseId(COMMAND.franchiseId())
                .name(COMMAND.name()).address(COMMAND.address())
                .city(COMMAND.city()).phone(COMMAND.phone())
                .status(StoreStatus.ACTIVE)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(franchiseRepositoryPort.existsById(COMMAND.franchiseId())).thenReturn(Mono.just(true));
        when(storeRepositoryPort.save(any(Store.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(createStoreService.execute(COMMAND))
                .assertNext(store -> {
                    assertThat(store.getFranchiseId()).isEqualTo(COMMAND.franchiseId());
                    assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);
                    assertThat(store.getId()).isNotBlank();
                    assertThat(store.getCreatedAt()).isNotNull();
                })
                .verifyComplete();

        verify(franchiseRepositoryPort).existsById(COMMAND.franchiseId());
        verify(storeRepositoryPort).save(any(Store.class));
    }

    @Test
    @DisplayName("execute: debe retornar ResourceNotFoundException cuando la franquicia no existe")
    void execute_shouldReturnResourceNotFoundException_whenFranchiseDoesNotExist() {
        when(franchiseRepositoryPort.existsById(COMMAND.franchiseId())).thenReturn(Mono.just(false));

        StepVerifier.create(createStoreService.execute(COMMAND))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResourceNotFoundException.class);
                    ResourceNotFoundException ex = (ResourceNotFoundException) error;
                    assertThat(ex.getField()).isEqualTo("franchiseId");
                    assertThat(ex.getValue()).isEqualTo(COMMAND.franchiseId());
                })
                .verify();

        verify(franchiseRepositoryPort).existsById(COMMAND.franchiseId());
        verify(storeRepositoryPort, never()).save(any());
    }
}
