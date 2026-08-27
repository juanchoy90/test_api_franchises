package com.juandavidg.franchise.infrastructure.adapter.in.rest;

import com.juandavidg.franchise.domain.exception.ResourceNotFoundException;
import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.ProductStatus;
import com.juandavidg.franchise.domain.port.in.CreateProductUseCase;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ProductResponse;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.exception.GlobalExceptionHandler;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.mapper.ProductWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private CreateProductUseCase createProductUseCase;

    @Mock
    private ProductWebMapper productWebMapper;

    private WebTestClient webTestClient;

    private static final String URL = "/api/v1/products";

    private Product product;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(createProductUseCase, productWebMapper);

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();

        product = Product.builder()
                .id("prod_987654321").storeId("store_789xyz").franchiseId("fran_123")
                .name("Camiseta Deportiva Roja").code("CAM-ROJ-M-01")
                .price(new BigDecimal("29.99"))
                .description("Camiseta para correr transpirable talla M")
                .category("Apparel").stock(50)
                .status(ProductStatus.ACTIVE)
                .createdAt(Instant.parse("2026-08-26T15:30:00Z"))
                .updatedAt(Instant.parse("2026-08-26T15:30:00Z"))
                .build();

        productResponse = new ProductResponse(
                "prod_987654321", "store_789xyz", "Camiseta Deportiva Roja", "CAM-ROJ-M-01",
                new BigDecimal("29.99"), "Camiseta para correr transpirable talla M", "Apparel", 50,
                "active", "2026-08-26T15:30:00Z", "2026-08-26T15:30:00Z"
        );
    }

    @Test
    @DisplayName("POST → 201 cuando la petición es válida")
    void create_shouldReturn201_whenValidRequest() {
        when(createProductUseCase.execute(any())).thenReturn(Mono.just(product));
        when(productWebMapper.toResponse(product)).thenReturn(productResponse);

        webTestClient.post().uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"storeId":"store_789xyz","name":"Camiseta Deportiva Roja","code":"CAM-ROJ-M-01",
                         "price":29.99,"description":"Camiseta para correr transpirable talla M",
                         "category":"Apparel","stock":50}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("prod_987654321")
                .jsonPath("$.storeId").isEqualTo("store_789xyz")
                .jsonPath("$.status").isEqualTo("active")
                .jsonPath("$.createdAt").isEqualTo("2026-08-26T15:30:00Z");
    }

    @Test
    @DisplayName("POST → 400 con errors[] cuando faltan campos obligatorios")
    void create_shouldReturn400WithErrors_whenRequiredFieldsAreMissing() {
        webTestClient.post().uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Camiseta Deportiva Roja"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.errors").isArray();
    }

    @Test
    @DisplayName("POST → 400 cuando el stock es negativo")
    void create_shouldReturn400WithErrors_whenStockIsNegative() {
        webTestClient.post().uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"storeId":"store_789xyz","name":"Camiseta Deportiva Roja","code":"CAM-ROJ-M-01",
                         "price":29.99,"description":"Camiseta para correr transpirable talla M",
                         "category":"Apparel","stock":-1}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.errors[0].field").isEqualTo("stock");
    }

    @Test
    @DisplayName("POST → 400 cuando el storeId no existe")
    void create_shouldReturn400_whenStoreDoesNotExist() {
        when(createProductUseCase.execute(any()))
                .thenReturn(Mono.error(new ResourceNotFoundException("Store", "storeId", "store_999")));

        webTestClient.post().uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"storeId":"store_999","name":"Camiseta Deportiva Roja","code":"CAM-ROJ-M-01",
                         "price":29.99,"description":"Camiseta para correr transpirable talla M",
                         "category":"Apparel","stock":50}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("REFERENCED_RESOURCE_NOT_FOUND")
                .jsonPath("$.errors[0].field").isEqualTo("storeId");
    }
}
