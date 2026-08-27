package com.juandavidg.franchise.application;

import com.juandavidg.franchise.domain.exception.FranchiseNotFoundException;
import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.port.in.GetTopStockProductsPerStoreUseCase;
import com.juandavidg.franchise.domain.port.out.FranchiseRepositoryPort;
import com.juandavidg.franchise.domain.port.out.ProductRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetTopStockProductsPerStoreService implements GetTopStockProductsPerStoreUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final FranchiseRepositoryPort franchiseRepositoryPort;

    @Override
    public Flux<Product> execute(String franchiseId) {
        log.info("Checking franchise existence for franchiseId={}", franchiseId);

        return franchiseRepositoryPort.existsById(franchiseId)
                .flatMapMany(exists -> {
                    if (Boolean.FALSE.equals(exists)) {
                        log.warn("Top-stock-per-store query rejected, franchiseId={} does not exist", franchiseId);
                        return Flux.error(new FranchiseNotFoundException(franchiseId));
                    }
                    return productRepositoryPort.findAllByFranchiseId(franchiseId)
                            .collectList()
                            .flatMapMany(products -> Flux.fromIterable(topStockPerStore(products)));
                })
                .doOnComplete(() -> log.info("Top-stock-per-store query completed for franchiseId={}", franchiseId));
    }

    private List<Product> topStockPerStore(List<Product> products) {
        Map<String, Product> topByStore = new LinkedHashMap<>();
        for (Product product : products) {
            topByStore.merge(product.getStoreId(), product,
                    (current, candidate) -> candidate.getStock() > current.getStock() ? candidate : current);
        }
        return List.copyOf(topByStore.values());
    }
}
