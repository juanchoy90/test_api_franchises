package com.juandavidg.franchise.domain.port.in;

import com.juandavidg.franchise.domain.model.Store;
import com.juandavidg.franchise.domain.model.command.CreateStoreCommand;
import reactor.core.publisher.Mono;

public interface CreateStoreUseCase {

    Mono<Store> execute(CreateStoreCommand command);
}
