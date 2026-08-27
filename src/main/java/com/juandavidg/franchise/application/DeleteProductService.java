package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.ProductNotFoundException;
import com.juandavidg.franchise.domain.model.command.DeleteProductCommand;
import com.juandavidg.franchise.domain.port.in.DeleteProductUseCase;
import com.juandavidg.franchise.domain.port.out.ProductRepositoryPort;
import com.juandavidg.franchise.domain.port.out.StoreRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteProductService implements DeleteProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final StoreRepositoryPort storeRepositoryPort;

    @Override
    public Mono<Void> execute(DeleteProductCommand command) {
        log.info("Checking store existence for storeId={} before deleting productId={}", command.storeId(), command.productId());

        return storeRepositoryPort.findFranchiseIdByStoreId(command.storeId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Product deletion rejected, storeId={} does not exist", command.storeId());
                    return Mono.error(new ProductNotFoundException(command.storeId(), command.productId()));
                }))
                .flatMap(franchiseId -> productRepositoryPort.deleteById(franchiseId, command.storeId(), command.productId()))
                .doOnSuccess(v -> log.info("Product deleted successfully id={} storeId={}", command.productId(), command.storeId()));
    }
}
