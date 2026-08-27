package com.juandavidg.franchise.domain.port.out;

import com.juandavidg.franchise.domain.model.Product;
import reactor.core.publisher.Mono;

public interface ProductRepositoryPort {

    Mono<Product> save(Product product);

    Mono<Void> deleteById(String franchiseId, String storeId, String productId);
}
