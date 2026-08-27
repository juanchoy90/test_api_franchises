package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.ResourceNotFoundException;
import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.ProductStatus;
import com.juandavidg.franchise.domain.model.command.CreateProductCommand;
import com.juandavidg.franchise.domain.port.in.CreateProductUseCase;
import com.juandavidg.franchise.domain.port.out.ProductRepositoryPort;
import com.juandavidg.franchise.domain.port.out.StoreRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateProductService implements CreateProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final StoreRepositoryPort storeRepositoryPort;

    @Override
    public Mono<Product> execute(CreateProductCommand command) {
        log.info("Checking store existence for storeId={}", command.storeId());

        return storeRepositoryPort.findFranchiseIdByStoreId(command.storeId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Product creation rejected, storeId={} does not exist", command.storeId());
                    return Mono.error(new ResourceNotFoundException("Store", "storeId", command.storeId()));
                }))
                .flatMap(franchiseId -> {
                    Instant now = Instant.now();
                    Product product = Product.builder()
                            .id(UUID.randomUUID().toString())
                            .storeId(command.storeId())
                            .franchiseId(franchiseId)
                            .name(command.name())
                            .code(command.code())
                            .price(command.price())
                            .description(command.description())
                            .category(command.category())
                            .stock(command.stock())
                            .status(ProductStatus.ACTIVE)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    log.debug("Persisting new product id={} storeId={}", product.getId(), product.getStoreId());
                    return productRepositoryPort.save(product);
                })
                .doOnNext(product -> log.info("Product created successfully id={} storeId={}", product.getId(), product.getStoreId()));
    }
}
