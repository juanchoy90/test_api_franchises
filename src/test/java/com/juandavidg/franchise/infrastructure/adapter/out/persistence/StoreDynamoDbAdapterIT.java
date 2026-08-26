package com.juandavidg.franchise.infrastructure.adapter.out.persistence;

import com.juandavidg.franchise.domain.model.Store;
import com.juandavidg.franchise.domain.model.StoreStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class StoreDynamoDbAdapterIT {

    static final String TABLE = "franchise-management";

    @Container
    static final LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3"))
            .withServices("dynamodb");

    static DynamoDbAsyncClient client;
    static StoreDynamoDbAdapter adapter;

    @BeforeAll
    static void setUpAll() {
        client = DynamoDbAsyncClient.builder()
                .endpointOverride(localStack.getEndpoint())
                .region(Region.of(localStack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .build();

        client.createTable(CreateTableRequest.builder()
                .tableName(TABLE)
                .keySchema(
                        KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName("PK").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("SK").attributeType(ScalarAttributeType.S).build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build()).join();

        adapter = new StoreDynamoDbAdapter(client, TABLE);
    }

    @AfterEach
    void cleanTable() {
        client.scan(ScanRequest.builder()
                        .tableName(TABLE)
                        .projectionExpression("PK, SK")
                        .build())
                .thenCompose(response -> {
                    if (response.items().isEmpty()) {
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    }
                    List<WriteRequest> deletes = response.items().stream()
                            .map(item -> WriteRequest.builder()
                                    .deleteRequest(DeleteRequest.builder()
                                            .key(Map.of("PK", item.get("PK"), "SK", item.get("SK")))
                                            .build())
                                    .build())
                            .toList();
                    return client.batchWriteItem(b -> b.requestItems(Map.of(TABLE, deletes)));
                })
                .join();
    }

    @Test
    @DisplayName("save: debe persistir la sucursal con PK=FRANCHISE#{franchiseId} y SK=STORE#{id}")
    void save_shouldPersistStoreUnderFranchisePartition() {
        Store store = buildStore("store_001", "fran_123");

        StepVerifier.create(adapter.save(store))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isEqualTo("store_001");
                    assertThat(saved.getFranchiseId()).isEqualTo("fran_123");
                    assertThat(saved.getStatus()).isEqualTo(StoreStatus.ACTIVE);
                })
                .verifyComplete();

        Map<String, AttributeValue> item = client.getItem(GetItemRequest.builder()
                        .tableName(TABLE)
                        .key(Map.of(
                                "PK", AttributeValue.fromS("FRANCHISE#fran_123"),
                                "SK", AttributeValue.fromS("STORE#store_001")
                        ))
                        .build())
                .join()
                .item();

        assertThat(item.get("entityType").s()).isEqualTo("STORE");
        assertThat(item.get("id").s()).isEqualTo("store_001");
        assertThat(item.get("franchiseId").s()).isEqualTo("fran_123");
        assertThat(item.get("status").s()).isEqualTo("active");
        assertThat(item.get("metadata").m().get("name").s()).isEqualTo("Sucursal Norte");
        assertThat(item.get("metadata").m().get("address").s()).isEqualTo("Cra 7 # 45-12");
        assertThat(item.get("metadata").m().get("city").s()).isEqualTo("Bogotá");
        assertThat(item.get("metadata").m().get("phone").s()).isEqualTo("6011234567");
    }

    private Store buildStore(String id, String franchiseId) {
        Instant now = Instant.now();
        return Store.builder()
                .id(id).franchiseId(franchiseId)
                .name("Sucursal Norte").address("Cra 7 # 45-12")
                .city("Bogotá").phone("6011234567")
                .status(StoreStatus.ACTIVE)
                .createdAt(now).updatedAt(now)
                .build();
    }
}
