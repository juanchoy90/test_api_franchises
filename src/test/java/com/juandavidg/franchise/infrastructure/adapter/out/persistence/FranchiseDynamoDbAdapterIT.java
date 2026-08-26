package com.juandavidg.franchise.infrastructure.adapter.out.persistence;

import com.juandavidg.franchise.domain.exception.DuplicateResourceException;
import com.juandavidg.franchise.domain.model.Franchise;
import com.juandavidg.franchise.domain.model.FranchiseStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
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
class FranchiseDynamoDbAdapterIT {

    static final String TABLE = "franchise-management";

    @Container
    static final LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3"))
            .withServices("dynamodb");

    static DynamoDbAsyncClient client;
    static FranchiseDynamoDbAdapter adapter;

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

        adapter = new FranchiseDynamoDbAdapter(client, TABLE);
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
    @DisplayName("existsByNit: debe retornar false cuando el NIT no existe")
    void existsByNit_shouldReturnFalse_whenNitDoesNotExist() {
        StepVerifier.create(adapter.existsByNit("999.999.999-9"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("save: debe persistir la franquicia y el ítem de unicidad del NIT")
    void save_shouldPersistFranchiseAndNitUniqueItem() {
        Franchise franchise = buildFranchise("fran_001", "900.123.456-7");

        StepVerifier.create(adapter.save(franchise))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isEqualTo("fran_001");
                    assertThat(saved.getNit()).isEqualTo("900.123.456-7");
                    assertThat(saved.getStatus()).isEqualTo(FranchiseStatus.ACTIVE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("existsByNit: debe retornar true después de guardar una franquicia con ese NIT")
    void existsByNit_shouldReturnTrue_afterSave() {
        Franchise franchise = buildFranchise("fran_002", "900.123.456-7");

        StepVerifier.create(adapter.save(franchise)
                        .then(adapter.existsByNit("900.123.456-7")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("save: debe retornar DuplicateResourceException cuando el NIT ya existe")
    void save_shouldError_whenNitAlreadyExists() {
        Franchise first = buildFranchise("fran_003", "900.123.456-7");
        Franchise second = buildFranchise("fran_004", "900.123.456-7");

        StepVerifier.create(adapter.save(first).then(adapter.save(second)))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DuplicateResourceException.class);
                    DuplicateResourceException ex = (DuplicateResourceException) error;
                    assertThat(ex.getField()).isEqualTo("nit");
                    assertThat(ex.getValue()).isEqualTo("900.123.456-7");
                })
                .verify();
    }

    private Franchise buildFranchise(String id, String nit) {
        Instant now = Instant.now();
        return Franchise.builder()
                .id(id).name("Franquicia Central").nit(nit)
                .city("Bogotá").country("Colombia").email("contacto@franquiciacentral.com")
                .status(FranchiseStatus.ACTIVE)
                .createdAt(now).updatedAt(now)
                .build();
    }
}
