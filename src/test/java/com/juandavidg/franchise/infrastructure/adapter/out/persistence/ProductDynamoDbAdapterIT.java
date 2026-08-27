package com.juandavidg.franchise.infrastructure.adapter.out.persistence;

import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.ProductStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ProductDynamoDbAdapterIT {

    static final String TABLE = "franchise-management";

    @Container
    static final LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3"))
            .withServices("dynamodb");

    static DynamoDbAsyncClient client;
    static ProductDynamoDbAdapter adapter;

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

        adapter = new ProductDynamoDbAdapter(client, TABLE);
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
    @DisplayName("save: debe persistir el producto con PK=FRANCHISE#{franchiseId} y SK=STORE#{storeId}#PRODUCT#{id}")
    void save_shouldPersistProductUnderStorePartition() {
        Product product = buildProduct("prod_001", "store_789xyz", "fran_123");

        StepVerifier.create(adapter.save(product))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isEqualTo("prod_001");
                    assertThat(saved.getStoreId()).isEqualTo("store_789xyz");
                    assertThat(saved.getStatus()).isEqualTo(ProductStatus.ACTIVE);
                })
                .verifyComplete();

        Map<String, AttributeValue> item = client.getItem(GetItemRequest.builder()
                        .tableName(TABLE)
                        .key(Map.of(
                                "PK", AttributeValue.fromS("FRANCHISE#fran_123"),
                                "SK", AttributeValue.fromS("STORE#store_789xyz#PRODUCT#prod_001")
                        ))
                        .build())
                .join()
                .item();

        assertThat(item.get("entityType").s()).isEqualTo("PRODUCT");
        assertThat(item.get("id").s()).isEqualTo("prod_001");
        assertThat(item.get("storeId").s()).isEqualTo("store_789xyz");
        assertThat(item.get("status").s()).isEqualTo("active");
        assertThat(item.get("price").n()).isEqualTo("29.99");
        assertThat(item.get("stock").n()).isEqualTo("50");
        assertThat(item.get("metadata").m().get("name").s()).isEqualTo("Camiseta Deportiva Roja");
        assertThat(item.get("metadata").m().get("code").s()).isEqualTo("CAM-ROJ-M-01");
        assertThat(item.get("metadata").m().get("description").s()).isEqualTo("Camiseta para correr transpirable talla M");
        assertThat(item.get("metadata").m().get("category").s()).isEqualTo("Apparel");
    }

    @Test
    @DisplayName("deleteById: debe eliminar físicamente el ítem del producto")
    void deleteById_shouldRemoveProductItem() {
        Product product = buildProduct("prod_002", "store_789xyz", "fran_123");
        StepVerifier.create(adapter.save(product)).expectNextCount(1).verifyComplete();

        StepVerifier.create(adapter.deleteById("fran_123", "store_789xyz", "prod_002"))
                .verifyComplete();

        Map<String, AttributeValue> item = client.getItem(GetItemRequest.builder()
                        .tableName(TABLE)
                        .key(Map.of(
                                "PK", AttributeValue.fromS("FRANCHISE#fran_123"),
                                "SK", AttributeValue.fromS("STORE#store_789xyz#PRODUCT#prod_002")
                        ))
                        .build())
                .join()
                .item();

        assertThat(item).isEmpty();
    }

    @Test
    @DisplayName("deleteById: debe fallar con ProductNotFoundException cuando el producto no existe")
    void deleteById_shouldFailWithProductNotFoundException_whenProductDoesNotExist() {
        StepVerifier.create(adapter.deleteById("fran_123", "store_789xyz", "prod_does_not_exist"))
                .expectError(com.juandavidg.franchise.domain.exception.ProductNotFoundException.class)
                .verify();
    }

    private Product buildProduct(String id, String storeId, String franchiseId) {
        Instant now = Instant.now();
        return Product.builder()
                .id(id).storeId(storeId).franchiseId(franchiseId)
                .name("Camiseta Deportiva Roja").code("CAM-ROJ-M-01")
                .price(new BigDecimal("29.99"))
                .description("Camiseta para correr transpirable talla M")
                .category("Apparel").stock(50)
                .status(ProductStatus.ACTIVE)
                .createdAt(now).updatedAt(now)
                .build();
    }
}
