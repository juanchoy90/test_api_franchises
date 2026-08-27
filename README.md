# Franchise API

## Descripción
Prueba tecnica rest Api para la ggestión de franquicias sucursales y productos

## Arquitectura
          CLIENTE
                       │
                       ▼
              ┌─────────────────┐
              │ Spring WebFlux  │
              │   Controllers   │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │     Service     │
              │ Mono / Flux     │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ AWS SDK 2.x     │
              │ DynamoDB Async  │
              └────────┬────────┘
                       │
                       ▼
                 ┌───────────┐
                 │ DynamoDB  │
                 └───────────┘
## Tecnologías
Java 21
Spring Boot
Spring WebFlux
AWS SDK for Java 2.x
DynamoDB Async Client
Docker
JUnit 5
Mockito

## Requisitos
Spring Boot 4.1.1, Java 21 LTS, Maven 3.9.16.
Docker Desktop instalado y en ejecución.
Aws CLI

## Ejecutar localmente
- Levantar localstack--> docker compose up -d

- Crear la Tabla de Prueba-->aws --endpoint-url=http://localhost:4566 dynamodb create-table --table-name franchise-management --attribute-definitions AttributeName=PK,AttributeType=S AttributeName=SK,AttributeType=S --key-schema AttributeName=PK,KeyType=HASH AttributeName=SK,KeyType=RANGE --billing-mode PAY_PER_REQUEST --region us-east-1 --profile localstack

- Para ver visualemte la tabla-->
npx dynamodb-admin --dynamo-endpoint http://localhost:4566

- mvn spring-boot:run -Dspring-boot.run.profiles=local

## Docker

## Variables de entorno

## Base de datos

## API



...

## Swagger

## Tests

## Decisiones técnicas

## Deployment
