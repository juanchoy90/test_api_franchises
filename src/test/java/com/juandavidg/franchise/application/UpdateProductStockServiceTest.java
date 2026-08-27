package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.InsufficientStockException;
import com.juandavidg.franchise.domain.exception.ProductNotFoundException;
import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.ProductStatus;
import com.juandavidg.franchise.domain.model.command.UpdateProductStockCommand;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductStockServiceTest {

    @Mock
    private ProductRepositoryPort productRepositoryPort;

    @Mock
    private StoreRepositoryPort storeRepositoryPort;

    @InjectMocks
    private UpdateProductStockService updateProductStockService;

    private static final UpdateProductStockCommand COMMAND =
            new UpdateProductStockCommand("store_789xyz", "prod_987654321", -5);

    @Test
    @DisplayName("execute: debe aplicar el delta y retornar el producto actualizado cuando la tienda y el stock lo permiten")
    void execute_shouldUpdateStock_whenStoreExistsAndStockIsSufficient() {
        Product updated = Product.builder()
                .id(COMMAND.productId()).storeId(COMMAND.storeId()).franchiseId("fran_123")
                .name("Camiseta Deportiva Roja").code("CAM-ROJ-M-01")
                .price(new BigDecimal("29.99")).description("desc").category("Apparel")
                .stock(45)
                .status(ProductStatus.ACTIVE)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(storeRepositoryPort.findFranchiseIdByStoreId(COMMAND.storeId())).thenReturn(Mono.just("fran_123"));
        when(productRepositoryPort.updateStock("fran_123", COMMAND.storeId(), COMMAND.productId(), COMMAND.quantity()))
                .thenReturn(Mono.just(updated));

        StepVerifier.create(updateProductStockService.execute(COMMAND))
                .assertNext(product -> assertThat(product.getStock()).isEqualTo(45))
                .verifyComplete();

        verify(storeRepositoryPort).findFranchiseIdByStoreId(COMMAND.storeId());
        verify(productRepositoryPort).updateStock("fran_123", COMMAND.storeId(), COMMAND.productId(), COMMAND.quantity());
    }

    @Test
    @DisplayName("execute: debe retornar ProductNotFoundException cuando la tienda no existe")
    void execute_shouldReturnProductNotFoundException_whenStoreDoesNotExist() {
        when(storeRepositoryPort.findFranchiseIdByStoreId(COMMAND.storeId())).thenReturn(Mono.empty());

        StepVerifier.create(updateProductStockService.execute(COMMAND))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ProductNotFoundException.class);
                    ProductNotFoundException ex = (ProductNotFoundException) error;
                    assertThat(ex.getStoreId()).isEqualTo(COMMAND.storeId());
                    assertThat(ex.getProductId()).isEqualTo(COMMAND.productId());
                })
                .verify();

        verify(productRepositoryPort, never()).updateStock(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("execute: debe propagar InsufficientStockException cuando el delta dejaría el stock en negativo")
    void execute_shouldPropagateInsufficientStockException_whenStockWouldGoNegative() {
        when(storeRepositoryPort.findFranchiseIdByStoreId(COMMAND.storeId())).thenReturn(Mono.just("fran_123"));
        when(productRepositoryPort.updateStock("fran_123", COMMAND.storeId(), COMMAND.productId(), COMMAND.quantity()))
                .thenReturn(Mono.error(new InsufficientStockException(COMMAND.storeId(), COMMAND.productId(), COMMAND.quantity())));

        StepVerifier.create(updateProductStockService.execute(COMMAND))
                .expectError(InsufficientStockException.class)
                .verify();
    }
}
