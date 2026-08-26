package com.juandavidg.franchise.infrastructure.adapter.in.rest;

import com.juandavidg.franchise.domain.model.command.CreateStoreCommand;
import com.juandavidg.franchise.domain.port.in.CreateStoreUseCase;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.CreateStoreRequest;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ErrorResponse;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.StoreResponse;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.mapper.StoreWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Stores", description = "Operations for store management")
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@Slf4j
public class StoreController {

    private final CreateStoreUseCase createStoreUseCase;
    private final StoreWebMapper storeWebMapper;

    @Operation(
            summary = "Create a new store",
            description = "Registers a new store associated with an existing franchise, with an initial ACTIVE status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Store created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = StoreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or franchiseId does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public Mono<ResponseEntity<StoreResponse>> create(@Valid @RequestBody CreateStoreRequest request) {

        log.info("Received create store request for franchiseId={}", request.franchiseId());

        CreateStoreCommand command = new CreateStoreCommand(
                request.franchiseId(), request.name(), request.address(), request.city(), request.phone());

        return createStoreUseCase.execute(command)
                .map(store -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .<StoreResponse>body(storeWebMapper.toResponse(store)))
                .doOnNext(response -> log.info("Store creation request completed with status={}", response.getStatusCode()))
                .doOnError(ex -> log.error("Store creation request failed for franchiseId={}", request.franchiseId(), ex));
    }
}
