package com.juandavidg.franchise.infrastructure.adapter.in.rest;

import com.juandavidg.franchise.domain.exception.DuplicateResourceException;
import com.juandavidg.franchise.domain.model.Franchise;
import com.juandavidg.franchise.domain.model.FranchiseStatus;
import com.juandavidg.franchise.domain.port.in.CreateFranchiseUseCase;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.FranchiseResponse;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.exception.GlobalExceptionHandler;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.mapper.FranchiseWebMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FranchiseControllerTest {

    @Mock
    private CreateFranchiseUseCase createFranchiseUseCase;

    @Mock
    private FranchiseWebMapper franchiseWebMapper;

    private WebTestClient webTestClient;

    private static final String VALID_KEY = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d";
    private static final String URL = "/api/v1/franchises";

    private Franchise franchise;
    private FranchiseResponse franchiseResponse;

    @BeforeEach
    void setUp() {
        FranchiseController controller = new FranchiseController(createFranchiseUseCase, franchiseWebMapper);

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();

        franchise = Franchise.builder()
                .id("fran_123").name("Franquicia Central").nit("900.123.456-7")
                .city("Bogotá").country("Colombia").email("contacto@franquiciacentral.com")
                .status(FranchiseStatus.ACTIVE)
                .createdAt(Instant.parse("2026-08-26T16:22:00Z"))
                .updatedAt(Instant.parse("2026-08-26T16:22:00Z"))
                .build();

        franchiseResponse = new FranchiseResponse(
                "fran_123", "Franquicia Central", "900.123.456-7",
                "Bogotá", "Colombia", "contacto@franquiciacentral.com",
                "active", "2026-08-26T16:22:00Z", "2026-08-26T16:22:00Z"
        );
    }

    @Test
    @DisplayName("POST → 201 cuando la petición es válida")
    void create_shouldReturn201_whenValidRequest() {
        when(createFranchiseUseCase.execute(any())).thenReturn(Mono.just(franchise));
        when(franchiseWebMapper.toResponse(franchise)).thenReturn(franchiseResponse);

        webTestClient.post().uri(URL)
                .header(FranchiseController.IDEMPOTENCY_HEADER, VALID_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Franquicia Central","nit":"900.123.456-7",
                         "city":"Bogotá","country":"Colombia","email":"contacto@franquiciacentral.com"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("fran_123")
                .jsonPath("$.status").isEqualTo("active")
                .jsonPath("$.createdAt").isEqualTo("2026-08-26T16:22:00Z");
    }

    @Test
    @DisplayName("POST → 400 cuando falta el header Idempotency-Key")
    void create_shouldReturn400_whenIdempotencyKeyHeaderIsMissing() {
        webTestClient.post().uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Franquicia Central","nit":"900.123.456-7",
                         "city":"Bogotá","country":"Colombia","email":"contacto@franquiciacentral.com"}
                        """)
                .exchange()
                .expectStatus().isBadRequest();

        verify(createFranchiseUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST → 400 con errors[] cuando faltan campos obligatorios")
    void create_shouldReturn400WithErrors_whenRequiredFieldsAreMissing() {
        webTestClient.post().uri(URL)
                .header(FranchiseController.IDEMPOTENCY_HEADER, VALID_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"nit":"900.123.456-7"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.errors").isArray();
    }

    @Test
    @DisplayName("POST → 409 cuando el NIT ya existe")
    void create_shouldReturn409_whenNitAlreadyExists() {
        when(createFranchiseUseCase.execute(any()))
                .thenReturn(Mono.error(new DuplicateResourceException("Franchise", "nit", "900.123.456-7")));

        webTestClient.post().uri(URL)
                .header(FranchiseController.IDEMPOTENCY_HEADER, VALID_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Franquicia Central","nit":"900.123.456-7",
                         "city":"Bogotá","country":"Colombia","email":"contacto@franquiciacentral.com"}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DUPLICATE_RESOURCE")
                .jsonPath("$.errors[0].field").isEqualTo("nit");
    }
}
