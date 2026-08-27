package com.juandavidg.franchise.infrastructure.adapter.out.persistence;

import com.juandavidg.franchise.domain.model.Store;
import com.juandavidg.franchise.domain.port.out.StoreRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

import java.util.Map;

@Repository
@Slf4j
public class StoreDynamoDbAdapter implements StoreRepositoryPort {

    private static final String PREFIX_FRANCHISE = "FRANCHISE#";
    private static final String PREFIX_STORE = "STORE#";
    private static final String SK_METADATA = "METADATA";

    private final DynamoDbAsyncClient dynamoDbClient;
    private final String tableName;

    public StoreDynamoDbAdapter(DynamoDbAsyncClient dynamoDbClient,
                                 @Value("${aws.dynamodb.table-name}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public Mono<Store> save(Store store) {
        log.debug("Saving store item to DynamoDB, id={} franchiseId={}", store.getId(), store.getFranchiseId());

        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                .transactItems(
                        TransactWriteItem.builder().put(mainItem(store)).build(),
                        TransactWriteItem.builder().put(lookupItem(store)).build()
                )
                .build();

        return Mono.fromFuture(() -> dynamoDbClient.transactWriteItems(request))
                .thenReturn(store);
    }

    @Override
    public Mono<String> findFranchiseIdByStoreId(String storeId) {
        log.debug("Looking up franchiseId for storeId={} in DynamoDB", storeId);

        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "PK", s(PREFIX_STORE + storeId),
                        "SK", s(SK_METADATA)
                ))
                .projectionExpression("franchiseId")
                .build();

        return Mono.fromFuture(() -> dynamoDbClient.getItem(request))
                .flatMap(response -> response.hasItem() && !response.item().isEmpty()
                        ? Mono.just(response.item().get("franchiseId").s())
                        : Mono.empty());
    }

    private Put mainItem(Store store) {
        return Put.builder()
                .tableName(tableName)
                .item(Map.of(
                        "PK",         s(PREFIX_FRANCHISE + store.getFranchiseId()),
                        "SK",         s(PREFIX_STORE + store.getId()),
                        "entityType", s("STORE"),
                        "id",         s(store.getId()),
                        "franchiseId", s(store.getFranchiseId()),
                        "status",     s(store.getStatus().name().toLowerCase()),
                        "createdAt",  s(store.getCreatedAt().toString()),
                        "updatedAt",  s(store.getUpdatedAt().toString()),
                        "metadata",   AttributeValue.fromM(Map.of(
                                "name",    s(store.getName()),
                                "address", s(store.getAddress()),
                                "city",    s(store.getCity()),
                                "phone",   s(store.getPhone())
                        ))
                ))
                .build();
    }

    private Put lookupItem(Store store) {
        return Put.builder()
                .tableName(tableName)
                .item(Map.of(
                        "PK",          s(PREFIX_STORE + store.getId()),
                        "SK",          s(SK_METADATA),
                        "entityType",  s("STORE_LOOKUP"),
                        "franchiseId", s(store.getFranchiseId())
                ))
                .build();
    }

    private static AttributeValue s(String value) {
        return AttributeValue.fromS(value);
    }
}
