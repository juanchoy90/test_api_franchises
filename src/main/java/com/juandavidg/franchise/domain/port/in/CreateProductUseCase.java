package com.juandavidg.franchise.domain.port.in;

import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.command.CreateProductCommand;
import reactor.core.publisher.Mono;

public interface CreateProductUseCase {

    Mono<Product> execute(CreateProductCommand command);
}
