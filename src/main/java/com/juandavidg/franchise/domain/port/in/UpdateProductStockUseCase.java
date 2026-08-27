package com.juandavidg.franchise.domain.port.in;

import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.command.UpdateProductStockCommand;
import reactor.core.publisher.Mono;

public interface UpdateProductStockUseCase {

    Mono<Product> execute(UpdateProductStockCommand command);
}
