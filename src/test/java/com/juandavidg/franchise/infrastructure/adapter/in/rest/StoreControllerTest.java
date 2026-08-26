package com.juandavidg.franchise.infrastructure.adapter.in.rest;

import com.juandavidg.franchise.domain.exception.ResourceNotFoundException;
import com.juandavidg.franchise.domain.model.Store;
import com.juandavidg.franchise.domain.model.StoreStatus;
import com.juandavidg.franchise.domain.port.in.CreateStoreUseCase;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.StoreResponse;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.exception.GlobalExceptionHandler;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.mapper.StoreWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreControllerTest {

    @Mock
    private CreateStoreUseCase createStoreUseCase;

    @Mock
    private StoreWebMapper storeWebMapper;

    private WebTestClient webTestClient;

    private static final String URL = "/api/v1/stores";

    private Store store;
    private StoreResponse storeResponse;

    @BeforeEach
    void setUp() {
        StoreController controller = new StoreController(createStoreUseCase, storeWebMapper);

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();

        store = Store.builder()
                .id("store_123").franchiseId("fran_123")
                .name("Sucursal Norte").address("Cra 7 # 45-12")
                .city("Bogotá").phone("6011234567")
                .status(StoreStatus.ACTIVE)
                .createdAt(Instant.parse("2026-08-26T16:22:00Z"))
                .updatedAt(Instant.parse("2026-08-26T16:22:00Z"))
                .build();

        storeResponse = new StoreResponse(
                "store_123", "fran_123", "Sucursal Norte", "Cra 7 # 45-12",
                "Bogotá", "6011234567", "active", "2026-08-26T16:22:00Z", "2026-08-26T16:22:00Z"
        );
    }

    @Test
    @DisplayName("POST → 201 cuando la petición es válida")
    void create_shouldReturn201_whenValidRequest() {
        when(createStoreUseCase.execute(any())).thenReturn(Mono.just(store));
        when(storeWebMapper.toResponse(store)).thenReturn(storeResponse);

        webTestClient.post().uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"franchiseId":"fran_123","name":"Sucursal Norte",
                         "address":"Cra 7 # 45-12","city":"Bogotá","phone":"6011234567"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("store_123")
                .jsonPath("$.franchiseId").isEqualTo("fran_123")
                .jsonPath("$.status").isEqualTo("active")
                .jsonPath("$.createdAt").isEqualTo("2026-08-26T16:22:00Z");
    }

    @Test
    @DisplayName("POST → 400 con errors[] cuando faltan campos obligatorios")
    void create_shouldReturn400WithErrors_whenRequiredFieldsAreMissing() {
        webTestClient.post().uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Sucursal Norte"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.errors").isArray();
    }

    @Test
    @DisplayName("POST → 400 cuando el franchiseId no existe")
    void create_shouldReturn400_whenFranchiseDoesNotExist() {
        when(createStoreUseCase.execute(any()))
                .thenReturn(Mono.error(new ResourceNotFoundException("Franchise", "franchiseId", "fran_999")));

        webTestClient.post().uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"franchiseId":"fran_999","name":"Sucursal Norte",
                         "address":"Cra 7 # 45-12","city":"Bogotá","phone":"6011234567"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("REFERENCED_RESOURCE_NOT_FOUND")
                .jsonPath("$.errors[0].field").isEqualTo("franchiseId");
    }
}
