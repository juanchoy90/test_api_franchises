package com.juandavidg.franchise.infrastructure.adapter.out.persistence;

import com.juandavidg.franchise.domain.model.Store;
import com.juandavidg.franchise.domain.port.out.StoreRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

@Repository
@Slf4j
public class StoreDynamoDbAdapter implements StoreRepositoryPort {

    private static final String PREFIX_FRANCHISE = "FRANCHISE#";
    private static final String PREFIX_STORE = "STORE#";

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

        PutItemRequest request = PutItemRequest.builder()
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

        return Mono.fromFuture(() -> dynamoDbClient.putItem(request))
                .thenReturn(store);
    }

    private static AttributeValue s(String value) {
        return AttributeValue.fromS(value);
    }
}
