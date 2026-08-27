package com.juandavidg.franchise.infrastructure.adapter.in.rest;

import com.juandavidg.franchise.domain.model.command.CreateProductCommand;
import com.juandavidg.franchise.domain.port.in.CreateProductUseCase;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.CreateProductRequest;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ErrorResponse;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ProductResponse;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.mapper.ProductWebMapper;
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

@Tag(name = "Products", description = "Operations for product management")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final ProductWebMapper productWebMapper;

    @Operation(
            summary = "Create a new product",
            description = "Registers a new product associated with an existing store, with an initial ACTIVE status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or storeId does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public Mono<ResponseEntity<ProductResponse>> create(@Valid @RequestBody CreateProductRequest request) {

        log.info("Received create product request for storeId={}", request.storeId());

        CreateProductCommand command = new CreateProductCommand(
                request.storeId(), request.name(), request.code(), request.price(),
                request.description(), request.category(), request.stock());

        return createProductUseCase.execute(command)
                .map(product -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .<ProductResponse>body(productWebMapper.toResponse(product)))
                .doOnNext(response -> log.info("Product creation request completed with status={}", response.getStatusCode()))
                .doOnError(ex -> log.error("Product creation request failed for storeId={}", request.storeId(), ex));
    }
}
