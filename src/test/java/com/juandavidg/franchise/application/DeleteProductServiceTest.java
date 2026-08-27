package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.ProductNotFoundException;
import com.juandavidg.franchise.domain.model.command.DeleteProductCommand;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteProductServiceTest {

    @Mock
    private ProductRepositoryPort productRepositoryPort;

    @Mock
    private StoreRepositoryPort storeRepositoryPort;

    @InjectMocks
    private DeleteProductService deleteProductService;

    private static final DeleteProductCommand COMMAND = new DeleteProductCommand("store_789xyz", "prod_987654321");

    @Test
    @DisplayName("execute: debe eliminar el producto cuando la tienda y el producto existen")
    void execute_shouldDeleteProduct_whenStoreAndProductExist() {
        when(storeRepositoryPort.findFranchiseIdByStoreId(COMMAND.storeId())).thenReturn(Mono.just("fran_123"));
        when(productRepositoryPort.deleteById("fran_123", COMMAND.storeId(), COMMAND.productId())).thenReturn(Mono.empty());

        StepVerifier.create(deleteProductService.execute(COMMAND))
                .verifyComplete();

        verify(storeRepositoryPort).findFranchiseIdByStoreId(COMMAND.storeId());
        verify(productRepositoryPort).deleteById("fran_123", COMMAND.storeId(), COMMAND.productId());
    }

    @Test
    @DisplayName("execute: debe retornar ProductNotFoundException cuando la tienda no existe")
    void execute_shouldReturnProductNotFoundException_whenStoreDoesNotExist() {
        when(storeRepositoryPort.findFranchiseIdByStoreId(COMMAND.storeId())).thenReturn(Mono.empty());

        StepVerifier.create(deleteProductService.execute(COMMAND))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ProductNotFoundException.class);
                    ProductNotFoundException ex = (ProductNotFoundException) error;
                    assertThat(ex.getStoreId()).isEqualTo(COMMAND.storeId());
                    assertThat(ex.getProductId()).isEqualTo(COMMAND.productId());
                })
                .verify();

        verify(storeRepositoryPort).findFranchiseIdByStoreId(COMMAND.storeId());
        verify(productRepositoryPort, never()).deleteById(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("execute: debe propagar ProductNotFoundException cuando el producto no existe en la tienda")
    void execute_shouldPropagateProductNotFoundException_whenProductDoesNotExistInStore() {
        when(storeRepositoryPort.findFranchiseIdByStoreId(COMMAND.storeId())).thenReturn(Mono.just("fran_123"));
        when(productRepositoryPort.deleteById("fran_123", COMMAND.storeId(), COMMAND.productId()))
                .thenReturn(Mono.error(new ProductNotFoundException(COMMAND.storeId(), COMMAND.productId())));

        StepVerifier.create(deleteProductService.execute(COMMAND))
                .expectError(ProductNotFoundException.class)
                .verify();
    }
}
