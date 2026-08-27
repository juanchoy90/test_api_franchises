package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.ProductNotFoundException;
import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.command.UpdateProductStockCommand;
import com.juandavidg.franchise.domain.port.in.UpdateProductStockUseCase;
import com.juandavidg.franchise.domain.port.out.ProductRepositoryPort;
import com.juandavidg.franchise.domain.port.out.StoreRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateProductStockService implements UpdateProductStockUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final StoreRepositoryPort storeRepositoryPort;

    @Override
    public Mono<Product> execute(UpdateProductStockCommand command) {
        log.info("Checking store existence for storeId={} before updating stock of productId={}",
                command.storeId(), command.productId());

        return storeRepositoryPort.findFranchiseIdByStoreId(command.storeId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Stock update rejected, storeId={} does not exist", command.storeId());
                    return Mono.error(new ProductNotFoundException(command.storeId(), command.productId()));
                }))
                .flatMap(franchiseId -> productRepositoryPort.updateStock(
                        franchiseId, command.storeId(), command.productId(), command.quantity()))
                .doOnNext(product -> log.info("Stock updated successfully id={} storeId={} newStock={}",
                        product.getId(), product.getStoreId(), product.getStock()));
    }
}
