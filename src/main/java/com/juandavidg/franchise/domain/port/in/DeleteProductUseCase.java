package com.juandavidg.franchise.domain.port.in;

import com.juandavidg.franchise.domain.model.command.DeleteProductCommand;
import reactor.core.publisher.Mono;

public interface DeleteProductUseCase {

    Mono<Void> execute(DeleteProductCommand command);
}
