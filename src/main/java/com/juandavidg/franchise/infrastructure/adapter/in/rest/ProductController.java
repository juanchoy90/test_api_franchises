package com.juandavidg.franchise.infrastructure.adapter.in.rest;

import com.juandavidg.franchise.domain.model.command.CreateProductCommand;
import com.juandavidg.franchise.domain.model.command.DeleteProductCommand;
import com.juandavidg.franchise.domain.model.command.UpdateProductStockCommand;
import com.juandavidg.franchise.domain.port.in.CreateProductUseCase;
import com.juandavidg.franchise.domain.port.in.DeleteProductUseCase;
import com.juandavidg.franchise.domain.port.in.GetTopStockProductsPerStoreUseCase;
import com.juandavidg.franchise.domain.port.in.UpdateProductStockUseCase;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.CreateProductRequest;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ErrorResponse;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ProductResponse;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.UpdateProductStockRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Tag(name = "Products", description = "Operations for product management")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final UpdateProductStockUseCase updateProductStockUseCase;
    private final GetTopStockProductsPerStoreUseCase getTopStockProductsPerStoreUseCase;
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

    @Operation(
            summary = "Delete a product",
            description = "Permanently deletes a product from a store's catalog."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product does not exist in the given store",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{productId}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String productId, @RequestParam String storeId) {

        log.info("Received delete product request for productId={} storeId={}", productId, storeId);

        DeleteProductCommand command = new DeleteProductCommand(storeId, productId);

        return deleteProductUseCase.execute(command)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .doOnNext(response -> log.info("Product deletion request completed with status={}", response.getStatusCode()))
                .doOnError(ex -> log.error("Product deletion request failed for productId={} storeId={}", productId, storeId, ex));
    }

    @Operation(
            summary = "Update product stock",
            description = "Applies a signed delta (positive to restock, negative to consume) to a product's stock atomically."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product does not exist in the given store",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The requested delta would leave the stock negative",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{productId}/stock")
    public Mono<ResponseEntity<ProductResponse>> updateStock(@PathVariable String productId,
                                                               @RequestParam String storeId,
                                                               @Valid @RequestBody UpdateProductStockRequest request) {

        log.info("Received update stock request for productId={} storeId={} quantity={}",
                productId, storeId, request.quantity());

        UpdateProductStockCommand command = new UpdateProductStockCommand(storeId, productId, request.quantity());

        return updateProductStockUseCase.execute(command)
                .map(product -> ResponseEntity.ok(productWebMapper.toResponse(product)))
                .doOnNext(response -> log.info("Stock update request completed with status={}", response.getStatusCode()))
                .doOnError(ex -> log.error("Stock update request failed for productId={} storeId={}", productId, storeId, ex));
    }

    @Operation(
            summary = "Get the top-stock product per store for a franchise",
            description = "For each store in the given franchise, returns the product with the highest stock. " +
                    "Stores without any products are omitted from the result."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of top-stock products, one per store",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Franchise does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/top-stock")
    public Mono<ResponseEntity<List<ProductResponse>>> getTopStockPerStore(@RequestParam String franchiseId) {

        log.info("Received get top-stock-per-store request for franchiseId={}", franchiseId);

        return getTopStockProductsPerStoreUseCase.execute(franchiseId)
                .map(productWebMapper::toResponse)
                .collectList()
                .map(ResponseEntity::ok)
                .doOnNext(response -> log.info("Top-stock-per-store request completed with status={}", response.getStatusCode()))
                .doOnError(ex -> log.error("Top-stock-per-store request failed for franchiseId={}", franchiseId, ex));
    }
}
