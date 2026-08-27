package com.juandavidg.franchise.domain.port.out;

import com.juandavidg.franchise.domain.model.Store;
import reactor.core.publisher.Mono;

public interface StoreRepositoryPort {

    Mono<Store> save(Store store);

    Mono<String> findFranchiseIdByStoreId(String storeId);
}
