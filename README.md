# Franchise API

API REST reactiva para gestión de franquicias, sucursales y productos. Spring WebFlux + DynamoDB (Single Table Design), arquitectura hexagonal, TDD estricto.

## Stack
Java 21 · Spring Boot 4.1.1 (WebFlux) · AWS SDK v2 (DynamoDB Async) · MapStruct · Lombok · SpringDoc/Swagger · JUnit 5 + Mockito + Testcontainers/LocalStack · Docker · Terraform (AWS: ECS Fargate, ECR, DynamoDB, VPC)

## Probar en la nube
Desplegado en ECS Fargate con IP pública (sin auth delante todavía — ver [Seguridad](#seguridad)):

```bash
curl http://18.206.162.148:8080/api/v1/franchises \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d" \
  -d '{"name":"Franquicia Central","nit":"900.123.456-7","city":"Bogotá","country":"Colombia","email":"contacto@fc.com"}'
```
Swagger: `http://18.206.162.148:8080/swagger-ui.html`

## Correr localmente
```bash
docker compose up -d
aws --endpoint-url=http://localhost:4566 dynamodb create-table \
  --table-name franchise-management \
  --attribute-definitions AttributeName=PK,AttributeType=S AttributeName=SK,AttributeType=S \
  --key-schema AttributeName=PK,KeyType=HASH AttributeName=SK,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST --region us-east-1 --profile localstack
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
API en `http://localhost:8080`, Swagger en `/swagger-ui.html`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/franchises` | Crear franquicia (requiere header `Idempotency-Key`) |
| `POST` | `/api/v1/stores` | Crear sucursal |
| `POST` | `/api/v1/products` | Crear producto |
| `DELETE` | `/api/v1/products/{id}?storeId=` | Eliminar producto (físico) |
| `PATCH` | `/api/v1/products/{id}/stock?storeId=` | Modificar stock — body `{"quantity": ±N}`, delta atómico |
| `GET` | `/api/v1/products/top-stock?franchiseId=` | Producto con más stock por sucursal |

Contrato completo (requests, responses, códigos de error) en Swagger.

## Base de datos
DynamoDB Single Table Design, tabla `franchise-management`:

| PK | SK | Entidad |
|---|---|---|
| `FRANCHISE#{id}` | `METADATA` | Franchise |
| `FRANCHISE#{id}` | `STORE#{id}` | Store |
| `FRANCHISE#{id}` | `STORE#{id}#PRODUCT#{id}` | Product |
| `STORE#{id}` | `METADATA` | Puntero Store→Franchise (permite resolver un producto solo con `storeId`, sin `Scan`) |

## Tests
```bash
mvn test      # 37 unitarios
mvn verify    # + 16 de integración contra DynamoDB real (Testcontainers/LocalStack, requiere Docker)
```

## Deployment
Infra como código en `infra/` (Terraform): VPC + subnets públicas, ECR, ECS Fargate, 2 tablas DynamoDB. Ver `infra/README.md`.
```bash
cd infra/bootstrap && terraform init && terraform apply   # una vez, crea el bucket de state
cd infra && terraform init && terraform apply             # resto de la infra
docker build -t franchise-api . && docker push <ecr_repository_url>:latest
aws ecs update-service --cluster <cluster> --service <service> --force-new-deployment
```

## Seguridad
Acceso temporal y directo por IP pública al puerto 8080 (`0.0.0.0/0` en el security group, marcado como `TEMPORAL` en `infra/modules/networking/main.tf`). El diseño original contempla Cognito + API Gateway + Lambda authorizer delante de ECS vía VPC Link, pero no se implementó por tiempo — **no apto para producción tal cual está**.

## Pendiente
- Idempotencia (`POST /franchises`): el header es obligatorio pero no hay cache/replay real — falta el adaptador de `IdempotencyPort` (la tabla DynamoDB ya existe en la infra, sin usar).
- Capa de seguridad (Cognito / API Gateway / Lambda authorizer).
