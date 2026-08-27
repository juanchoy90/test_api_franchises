package com.juandavidg.franchise.infrastructure.adapter.in.rest;

import com.juandavidg.franchise.domain.exception.FranchiseNotFoundException;
import com.juandavidg.franchise.domain.exception.InsufficientStockException;
import com.juandavidg.franchise.domain.exception.ProductNotFoundException;
import com.juandavidg.franchise.domain.exception.ResourceNotFoundException;
import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.ProductStatus;
import com.juandavidg.franchise.domain.port.in.CreateProductUseCase;
import com.juandavidg.franchise.domain.port.in.DeleteProductUseCase;
import com.juandavidg.franchise.domain.port.in.GetTopStockProductsPerStoreUseCase;
import com.juandavidg.franchise.domain.port.in.UpdateProductStockUseCase;
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
import reactor.core.publisher.Flux;
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
    private DeleteProductUseCase deleteProductUseCase;

    @Mock
    private UpdateProductStockUseCase updateProductStockUseCase;

    @Mock
    private GetTopStockProductsPerStoreUseCase getTopStockProductsPerStoreUseCase;

    @Mock
    private ProductWebMapper productWebMapper;

    private WebTestClient webTestClient;

    private static final String URL = "/api/v1/products";

    private Product product;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(
                createProductUseCase, deleteProductUseCase, updateProductStockUseCase,
                getTopStockProductsPerStoreUseCase, productWebMapper);

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

    @Test
    @DisplayName("DELETE → 204 cuando el producto y la tienda existen")
    void delete_shouldReturn204_whenProductAndStoreExist() {
        when(deleteProductUseCase.execute(any())).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path(URL + "/{productId}")
                        .queryParam("storeId", "store_789xyz")
                        .build("prod_987654321"))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("DELETE → 404 cuando el producto no existe en la tienda")
    void delete_shouldReturn404_whenProductDoesNotExist() {
        when(deleteProductUseCase.execute(any()))
                .thenReturn(Mono.error(new ProductNotFoundException("store_789xyz", "prod_999")));

        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path(URL + "/{productId}")
                        .queryParam("storeId", "store_789xyz")
                        .build("prod_999"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND")
                .jsonPath("$.errors[0].field").isEqualTo("productId");
    }

    @Test
    @DisplayName("DELETE → 400 cuando falta el query param storeId")
    void delete_shouldReturn400_whenStoreIdQueryParamIsMissing() {
        webTestClient.delete()
                .uri(URL + "/prod_987654321")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MISSING_INPUT");
    }

    @Test
    @DisplayName("PATCH stock → 200 con el producto actualizado cuando el delta es válido")
    void updateStock_shouldReturn200_whenQuantityIsValid() {
        Product updated = Product.builder()
                .id("prod_987654321").storeId("store_789xyz").franchiseId("fran_123")
                .name("Camiseta Deportiva Roja").code("CAM-ROJ-M-01")
                .price(new BigDecimal("29.99")).description("desc").category("Apparel")
                .stock(45)
                .status(ProductStatus.ACTIVE)
                .createdAt(Instant.parse("2026-08-26T15:30:00Z"))
                .updatedAt(Instant.parse("2026-08-26T16:00:00Z"))
                .build();
        ProductResponse updatedResponse = new ProductResponse(
                "prod_987654321", "store_789xyz", "Camiseta Deportiva Roja", "CAM-ROJ-M-01",
                new BigDecimal("29.99"), "desc", "Apparel", 45,
                "active", "2026-08-26T15:30:00Z", "2026-08-26T16:00:00Z"
        );

        when(updateProductStockUseCase.execute(any())).thenReturn(Mono.just(updated));
        when(productWebMapper.toResponse(updated)).thenReturn(updatedResponse);

        webTestClient.patch()
                .uri(uriBuilder -> uriBuilder.path(URL + "/{productId}/stock")
                        .queryParam("storeId", "store_789xyz")
                        .build("prod_987654321"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"quantity": -5}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.stock").isEqualTo(45);
    }

    @Test
    @DisplayName("PATCH stock → 409 cuando el delta dejaría el stock en negativo")
    void updateStock_shouldReturn409_whenStockWouldGoNegative() {
        when(updateProductStockUseCase.execute(any()))
                .thenReturn(Mono.error(new InsufficientStockException("store_789xyz", "prod_987654321", -100)));

        webTestClient.patch()
                .uri(uriBuilder -> uriBuilder.path(URL + "/{productId}/stock")
                        .queryParam("storeId", "store_789xyz")
                        .build("prod_987654321"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"quantity": -100}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INSUFFICIENT_STOCK");
    }

    @Test
    @DisplayName("PATCH stock → 404 cuando el producto no existe en la tienda")
    void updateStock_shouldReturn404_whenProductDoesNotExist() {
        when(updateProductStockUseCase.execute(any()))
                .thenReturn(Mono.error(new ProductNotFoundException("store_789xyz", "prod_999")));

        webTestClient.patch()
                .uri(uriBuilder -> uriBuilder.path(URL + "/{productId}/stock")
                        .queryParam("storeId", "store_789xyz")
                        .build("prod_999"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"quantity": -5}
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    @DisplayName("PATCH stock → 400 cuando falta quantity")
    void updateStock_shouldReturn400_whenQuantityIsMissing() {
        webTestClient.patch()
                .uri(uriBuilder -> uriBuilder.path(URL + "/{productId}/stock")
                        .queryParam("storeId", "store_789xyz")
                        .build("prod_987654321"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.errors[0].field").isEqualTo("quantity");
    }

    @Test
    @DisplayName("GET top-stock → 200 con un producto por sucursal")
    void getTopStockPerStore_shouldReturn200WithOneProductPerStore() {
        Product productStoreA = Product.builder()
                .id("prod_a1").storeId("store_A").franchiseId("fran_123")
                .name("Producto A").code("CODE-A")
                .price(new BigDecimal("10.00")).description("desc").category("cat")
                .stock(40).status(ProductStatus.ACTIVE)
                .createdAt(Instant.parse("2026-08-26T15:30:00Z"))
                .updatedAt(Instant.parse("2026-08-26T15:30:00Z"))
                .build();
        ProductResponse responseA = new ProductResponse(
                "prod_a1", "store_A", "Producto A", "CODE-A",
                new BigDecimal("10.00"), "desc", "cat", 40,
                "active", "2026-08-26T15:30:00Z", "2026-08-26T15:30:00Z"
        );

        when(getTopStockProductsPerStoreUseCase.execute("fran_123")).thenReturn(Flux.just(productStoreA));
        when(productWebMapper.toResponse(productStoreA)).thenReturn(responseA);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(URL + "/top-stock")
                        .queryParam("franchiseId", "fran_123")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].storeId").isEqualTo("store_A")
                .jsonPath("$[0].id").isEqualTo("prod_a1")
                .jsonPath("$[0].stock").isEqualTo(40);
    }

    @Test
    @DisplayName("GET top-stock → 404 cuando la franquicia no existe")
    void getTopStockPerStore_shouldReturn404_whenFranchiseDoesNotExist() {
        when(getTopStockProductsPerStoreUseCase.execute("fran_999"))
                .thenReturn(Flux.error(new FranchiseNotFoundException("fran_999")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(URL + "/top-stock")
                        .queryParam("franchiseId", "fran_999")
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND")
                .jsonPath("$.errors[0].field").isEqualTo("franchiseId");
    }

    @Test
    @DisplayName("GET top-stock → 400 cuando falta el query param franchiseId")
    void getTopStockPerStore_shouldReturn400_whenFranchiseIdQueryParamIsMissing() {
        webTestClient.get()
                .uri(URL + "/top-stock")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MISSING_INPUT");
    }
}
