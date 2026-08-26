package com.juandavidg.franchise.infrastructure.adapter.in.rest;

import com.juandavidg.franchise.domain.model.command.CreateFranchiseCommand;
import com.juandavidg.franchise.domain.port.in.CreateFranchiseUseCase;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.CreateFranchiseRequest;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ErrorResponse;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.FranchiseResponse;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.mapper.FranchiseWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Franchises", description = "Operations for franchise management")
@RestController
@RequestMapping("/api/v1/franchises")
@RequiredArgsConstructor
public class FranchiseController {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final CreateFranchiseUseCase createFranchiseUseCase;
    private final FranchiseWebMapper franchiseWebMapper;

    @Operation(
            summary = "Create a new franchise",
            description = "Registers a new franchise with an initial ACTIVE status. " +
                    "Requires a UUIDv4 Idempotency-Key header to safely support retries."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Franchise created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FranchiseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or missing required header",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A franchise with the same NIT already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public Mono<ResponseEntity<FranchiseResponse>> create(
            @Parameter(description = "Unique UUIDv4 key to guarantee idempotent request processing",
                    required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Valid @RequestBody CreateFranchiseRequest request) {

        CreateFranchiseCommand command = new CreateFranchiseCommand(
                request.name(), request.nit(), request.city(), request.country(), request.email());

        return createFranchiseUseCase.execute(command)
                .map(franchise -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .<FranchiseResponse>body(franchiseWebMapper.toResponse(franchise)));
    }
}
