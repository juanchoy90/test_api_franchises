package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.ResourceNotFoundException;
import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.ProductStatus;
import com.juandavidg.franchise.domain.model.command.CreateProductCommand;
import com.juandavidg.franchise.domain.port.out.ProductRepositoryPort;
import com.juandavidg.franchise.domain.port.out.StoreRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductServiceTest {

    @Mock
    private ProductRepositoryPort productRepositoryPort;

    @Mock
    private StoreRepositoryPort storeRepositoryPort;

    @InjectMocks
    private CreateProductService createProductService;

    private static final CreateProductCommand COMMAND = new CreateProductCommand(
            "store_789xyz", "Camiseta Deportiva Roja", "CAM-ROJ-M-01",
            new BigDecimal("29.99"), "Camiseta para correr transpirable talla M", "Apparel", 50
    );

    @Test
    @DisplayName("execute: debe guardar y retornar el producto cuando la tienda existe")
    void execute_shouldSaveAndReturnProduct_whenStoreExists() {
        Product saved = Product.builder()
                .id(UUID.randomUUID().toString())
                .storeId(COMMAND.storeId())
                .franchiseId("fran_123")
                .name(COMMAND.name()).code(COMMAND.code())
                .price(COMMAND.price()).description(COMMAND.description())
                .category(COMMAND.category()).stock(COMMAND.stock())
                .status(ProductStatus.ACTIVE)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(storeRepositoryPort.findFranchiseIdByStoreId(COMMAND.storeId())).thenReturn(Mono.just("fran_123"));
        when(productRepositoryPort.save(any(Product.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(createProductService.execute(COMMAND))
                .assertNext(product -> {
                    assertThat(product.getStoreId()).isEqualTo(COMMAND.storeId());
                    assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
                    assertThat(product.getId()).isNotBlank();
                    assertThat(product.getCreatedAt()).isNotNull();
                })
                .verifyComplete();

        verify(storeRepositoryPort).findFranchiseIdByStoreId(COMMAND.storeId());
        verify(productRepositoryPort).save(any(Product.class));
    }

    @Test
    @DisplayName("execute: debe retornar ResourceNotFoundException cuando la tienda no existe")
    void execute_shouldReturnResourceNotFoundException_whenStoreDoesNotExist() {
        when(storeRepositoryPort.findFranchiseIdByStoreId(COMMAND.storeId())).thenReturn(Mono.empty());

        StepVerifier.create(createProductService.execute(COMMAND))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResourceNotFoundException.class);
                    ResourceNotFoundException ex = (ResourceNotFoundException) error;
                    assertThat(ex.getField()).isEqualTo("storeId");
                    assertThat(ex.getValue()).isEqualTo(COMMAND.storeId());
                })
                .verify();

        verify(storeRepositoryPort).findFranchiseIdByStoreId(COMMAND.storeId());
        verify(productRepositoryPort, never()).save(any());
    }
}
