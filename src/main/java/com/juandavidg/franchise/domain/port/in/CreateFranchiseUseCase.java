package com.juandavidg.franchise.domain.port.in;

import com.juandavidg.franchise.domain.model.Franchise;
import com.juandavidg.franchise.domain.model.command.CreateFranchiseCommand;
import reactor.core.publisher.Mono;

public interface CreateFranchiseUseCase {

    Mono<Franchise> execute(CreateFranchiseCommand command);
}
