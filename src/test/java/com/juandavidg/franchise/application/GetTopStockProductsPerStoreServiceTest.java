package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.FranchiseNotFoundException;
import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.ProductStatus;
import com.juandavidg.franchise.domain.port.out.FranchiseRepositoryPort;
import com.juandavidg.franchise.domain.port.out.ProductRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTopStockProductsPerStoreServiceTest {

    @Mock
    private ProductRepositoryPort productRepositoryPort;

    @Mock
    private FranchiseRepositoryPort franchiseRepositoryPort;

    @InjectMocks
    private GetTopStockProductsPerStoreService service;

    private static final String FRANCHISE_ID = "fran_123";

    @Test
    @DisplayName("execute: debe retornar un producto por sucursal, el de mayor stock")
    void execute_shouldReturnTopStockProductPerStore_whenFranchiseExists() {
        Product storeA_low = product("prod_a1", "store_A", 10);
        Product storeA_high = product("prod_a2", "store_A", 40);
        Product storeB_only = product("prod_b1", "store_B", 5);

        when(franchiseRepositoryPort.existsById(FRANCHISE_ID)).thenReturn(Mono.just(true));
        when(productRepositoryPort.findAllByFranchiseId(FRANCHISE_ID))
                .thenReturn(Flux.just(storeA_low, storeA_high, storeB_only));

        StepVerifier.create(service.execute(FRANCHISE_ID).collectList())
                .assertNext(result -> {
                    assertThat(result).hasSize(2);
                    assertThat(result).extracting(Product::getId).containsExactlyInAnyOrder("prod_a2", "prod_b1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("execute: en caso de empate debe conservar el primero encontrado")
    void execute_shouldKeepFirstEncountered_onStockTie() {
        Product first = product("prod_a1", "store_A", 20);
        Product tied = product("prod_a2", "store_A", 20);

        when(franchiseRepositoryPort.existsById(FRANCHISE_ID)).thenReturn(Mono.just(true));
        when(productRepositoryPort.findAllByFranchiseId(FRANCHISE_ID)).thenReturn(Flux.just(first, tied));

        StepVerifier.create(service.execute(FRANCHISE_ID).collectList())
                .assertNext(result -> {
                    assertThat(result).hasSize(1);
                    assertThat(result.get(0).getId()).isEqualTo("prod_a1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("execute: debe retornar lista vacía cuando la franquicia no tiene productos")
    void execute_shouldReturnEmptyList_whenFranchiseHasNoProducts() {
        when(franchiseRepositoryPort.existsById(FRANCHISE_ID)).thenReturn(Mono.just(true));
        when(productRepositoryPort.findAllByFranchiseId(FRANCHISE_ID)).thenReturn(Flux.empty());

        StepVerifier.create(service.execute(FRANCHISE_ID).collectList())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("execute: debe retornar FranchiseNotFoundException cuando la franquicia no existe")
    void execute_shouldReturnFranchiseNotFoundException_whenFranchiseDoesNotExist() {
        when(franchiseRepositoryPort.existsById(FRANCHISE_ID)).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute(FRANCHISE_ID))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(FranchiseNotFoundException.class);
                    assertThat(((FranchiseNotFoundException) error).getFranchiseId()).isEqualTo(FRANCHISE_ID);
                })
                .verify();
    }

    private Product product(String id, String storeId, int stock) {
        Instant now = Instant.now();
        return Product.builder()
                .id(id).storeId(storeId).franchiseId(FRANCHISE_ID)
                .name("Producto " + id).code("CODE-" + id)
                .price(new BigDecimal("10.00")).description("desc").category("cat")
                .stock(stock)
                .status(ProductStatus.ACTIVE)
                .createdAt(now).updatedAt(now)
                .build();
    }
}
