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

## Instalación

## Ejecutar localmente

## Docker

## Variables de entorno

## Base de datos

## API



...

## Swagger

## Tests

## Decisiones técnicas

## Deployment
