package com.juandavidg.franchise.domain.port.in;

import com.juandavidg.franchise.domain.model.Product;
import reactor.core.publisher.Flux;

public interface GetTopStockProductsPerStoreUseCase {

    Flux<Product> execute(String franchiseId);
}
