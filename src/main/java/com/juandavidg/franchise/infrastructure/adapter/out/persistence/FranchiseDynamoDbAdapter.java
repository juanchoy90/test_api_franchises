package com.juandavidg.franchise.infrastructure.adapter.out.persistence;

import com.juandavidg.franchise.domain.exception.DuplicateResourceException;
import com.juandavidg.franchise.domain.model.Franchise;
import com.juandavidg.franchise.domain.port.out.FranchiseRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.Map;

@Repository
public class FranchiseDynamoDbAdapter implements FranchiseRepositoryPort {

    private static final String PREFIX_FRANCHISE = "FRANCHISE#";
    private static final String PREFIX_NIT = "NIT#";
    private static final String SK_METADATA = "METADATA";
    private static final String SK_UNIQUE = "UNIQUE";

    private final DynamoDbAsyncClient dynamoDbClient;
    private final String tableName;

    public FranchiseDynamoDbAdapter(DynamoDbAsyncClient dynamoDbClient,
                                    @Value("${aws.dynamodb.table-name}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                .transactItems(
                        TransactWriteItem.builder().put(mainItem(franchise)).build(),
                        TransactWriteItem.builder().put(nitUniqueItem(franchise.getNit())).build()
                )
                .build();

        return Mono.fromFuture(() -> dynamoDbClient.transactWriteItems(request))
                .thenReturn(franchise)
                .onErrorMap(TransactionCanceledException.class, ex ->
                        new DuplicateResourceException("Franchise", "nit", franchise.getNit()));
    }

    @Override
    public Mono<Boolean> existsByNit(String nit) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "PK", s(PREFIX_NIT + nit),
                        "SK", s(SK_UNIQUE)
                ))
                .projectionExpression("PK")
                .build();

        return Mono.fromFuture(() -> dynamoDbClient.getItem(request))
                .map(response -> response.hasItem() && !response.item().isEmpty());
    }

    private Put mainItem(Franchise franchise) {
        return Put.builder()
                .tableName(tableName)
                .item(Map.of(
                        "PK",         s(PREFIX_FRANCHISE + franchise.getId()),
                        "SK",         s(SK_METADATA),
                        "entityType", s("FRANCHISE"),
                        "id",         s(franchise.getId()),
                        "nit",        s(franchise.getNit()),
                        "status",     s(franchise.getStatus().name().toLowerCase()),
                        "createdAt",  s(franchise.getCreatedAt().toString()),
                        "updatedAt",  s(franchise.getUpdatedAt().toString()),
                        "metadata",   AttributeValue.fromM(Map.of(
                                "name",    s(franchise.getName()),
                                "city",    s(franchise.getCity()),
                                "country", s(franchise.getCountry()),
                                "email",   s(franchise.getEmail())
                        ))
                ))
                .build();
    }

    private Put nitUniqueItem(String nit) {
        return Put.builder()
                .tableName(tableName)
                .item(Map.of(
                        "PK",         s(PREFIX_NIT + nit),
                        "SK",         s(SK_UNIQUE),
                        "entityType", s("FRANCHISE_NIT_UNIQUE")
                ))
                .conditionExpression("attribute_not_exists(PK)")
                .build();
    }

    private static AttributeValue s(String value) {
        return AttributeValue.fromS(value);
    }
}
