package com.juandavidg.franchise.infrastructure.adapter.out.persistence;

import com.juandavidg.franchise.domain.exception.InsufficientStockException;
import com.juandavidg.franchise.domain.exception.ProductNotFoundException;
import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.ProductStatus;
import com.juandavidg.franchise.domain.port.out.ProductRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Repository
@Slf4j
public class ProductDynamoDbAdapter implements ProductRepositoryPort {

    private static final String PREFIX_FRANCHISE = "FRANCHISE#";
    private static final String PREFIX_STORE = "STORE#";
    private static final String INFIX_PRODUCT = "#PRODUCT#";

    private final DynamoDbAsyncClient dynamoDbClient;
    private final String tableName;

    public ProductDynamoDbAdapter(DynamoDbAsyncClient dynamoDbClient,
                                   @Value("${aws.dynamodb.table-name}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public Mono<Product> save(Product product) {
        log.debug("Saving product item to DynamoDB, id={} storeId={}", product.getId(), product.getStoreId());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.ofEntries(
                        Map.entry("PK",         s(PREFIX_FRANCHISE + product.getFranchiseId())),
                        Map.entry("SK",         s(PREFIX_STORE + product.getStoreId() + INFIX_PRODUCT + product.getId())),
                        Map.entry("entityType", s("PRODUCT")),
                        Map.entry("id",         s(product.getId())),
                        Map.entry("storeId",    s(product.getStoreId())),
                        Map.entry("price",      n(product.getPrice().toString())),
                        Map.entry("stock",      n(product.getStock().toString())),
                        Map.entry("status",     s(product.getStatus().name().toLowerCase())),
                        Map.entry("createdAt",  s(product.getCreatedAt().toString())),
                        Map.entry("updatedAt",  s(product.getUpdatedAt().toString())),
                        Map.entry("metadata",   AttributeValue.fromM(Map.of(
                                "name",        s(product.getName()),
                                "code",        s(product.getCode()),
                                "description", s(product.getDescription()),
                                "category",    s(product.getCategory())
                        )))
                ))
                .build();

        return Mono.fromFuture(() -> dynamoDbClient.putItem(request))
                .thenReturn(product);
    }

    @Override
    public Mono<Void> deleteById(String franchiseId, String storeId, String productId) {
        log.debug("Deleting product item from DynamoDB, id={} storeId={}", productId, storeId);

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "PK", s(PREFIX_FRANCHISE + franchiseId),
                        "SK", s(PREFIX_STORE + storeId + INFIX_PRODUCT + productId)
                ))
                .conditionExpression("attribute_exists(PK)")
                .build();

        return Mono.fromFuture(() -> dynamoDbClient.deleteItem(request))
                .then()
                .onErrorMap(ConditionalCheckFailedException.class,
                        ex -> new ProductNotFoundException(storeId, productId));
    }

    @Override
    public Mono<Product> updateStock(String franchiseId, String storeId, String productId, int quantity) {
        log.debug("Updating stock for product id={} storeId={} quantity={}", productId, storeId, quantity);

        Map<String, AttributeValue> key = Map.of(
                "PK", s(PREFIX_FRANCHISE + franchiseId),
                "SK", s(PREFIX_STORE + storeId + INFIX_PRODUCT + productId)
        );

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression("ADD stock :qty SET updatedAt = :now")
                .conditionExpression("attribute_exists(PK) AND stock >= :negQty")
                .expressionAttributeValues(Map.of(
                        ":qty",    n(String.valueOf(quantity)),
                        ":negQty", n(String.valueOf(-quantity)),
                        ":now",    s(Instant.now().toString())
                ))
                .returnValues(ReturnValue.ALL_NEW)
                .build();

        return Mono.fromFuture(() -> dynamoDbClient.updateItem(request))
                .map(response -> toDomain(response.attributes()))
                .onErrorResume(ConditionalCheckFailedException.class,
                        ex -> resolveUpdateStockFailure(key, storeId, productId, quantity));
    }

    private Mono<Product> resolveUpdateStockFailure(Map<String, AttributeValue> key, String storeId,
                                                      String productId, int quantity) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        return Mono.fromFuture(() -> dynamoDbClient.getItem(request))
                .flatMap(response -> response.hasItem() && !response.item().isEmpty()
                        ? Mono.<Product>error(new InsufficientStockException(storeId, productId, quantity))
                        : Mono.error(new ProductNotFoundException(storeId, productId)));
    }

    private Product toDomain(Map<String, AttributeValue> item) {
        Map<String, AttributeValue> metadata = item.get("metadata").m();
        return Product.builder()
                .id(item.get("id").s())
                .storeId(item.get("storeId").s())
                .name(metadata.get("name").s())
                .code(metadata.get("code").s())
                .price(new BigDecimal(item.get("price").n()))
                .description(metadata.get("description").s())
                .category(metadata.get("category").s())
                .stock(Integer.parseInt(item.get("stock").n()))
                .status(ProductStatus.valueOf(item.get("status").s().toUpperCase()))
                .createdAt(Instant.parse(item.get("createdAt").s()))
                .updatedAt(Instant.parse(item.get("updatedAt").s()))
                .build();
    }

    private static AttributeValue s(String value) {
        return AttributeValue.fromS(value);
    }

    private static AttributeValue n(String value) {
        return AttributeValue.fromN(value);
    }
}
