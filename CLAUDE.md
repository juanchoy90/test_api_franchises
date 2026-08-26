## Reglas de trabajo (importante, no cambia)

- El usuario controla el repo y los commits. No utilizar comandos git sin solicitarlo
- Nunca ejecutar nada que implique construir (crear archivos, generar
  codigo) sin preguntar primero

- Ir historia por historia. No adelantar modelos/campos/endpoints de
  historias que no se han definido todavia, aunque parezcan logicos.
- Cuando una historia (HU) tenga ambiguedad de negocio, funcional o no
  funcional, señalarla explicitamente y proponer una decision razonada
  no asumir en silencio. 
- Contrato de respuestas HTTP (importante, verificar contra cada HU)

  ## Arquitectura
  - Hexagonal con puertos explicitos: domain/port/in/ (interfaces de casos de uso) y domain/port/out/ (interfaces de persistencia).



  ## Tecnologias
  - Reactivo: Spring WebFlux + AWS SDK v2 async para DynamoDB.
  - Spring Boot 4.1.1, Java 21 LTS, Maven 3.9.16.

## Codigo
  - MapStruct para todos los mappers (DTO <-> dominio, dominio <-> persistencia). Los mappers viven en infrastructure/, NUNCA en domain/ — mapear es un detalle de adaptador, no de negocio.
  - Lombok para reducir boilerplate en entidades de dominio y DTOs (@Getter, @Builder, etc.) — decision confirmada, se combina con MapStruct sin problema.
  - Validaciones en el request con Bean Validation (@NotBlank, @Email, etc.) — nunca validacion manual con ifs en el controller.
  - TDD estricto: el test se escribe ANTES que el codigo de produccion, siempre, sin excepcion. Flujo por cada pieza de comportamiento: (1) escribir el test que falla, (2) escribir el minimo codigo para que pase, (3) refactorizar si hace falta. Esto aplica tanto a pruebas unitarias (dominio, casos de uso) como de integracion (controllers, adaptadores).
  - Entidades de dominio son clases, no records — tienen identidad y invariantes encapsulados (updateStock(), updateName(), etc.). Los DTOs SI son records — datos inmutables puros, sin comportamiento.
  - Inyeccion de dependencias por constructor siempre, nunca @Autowired en campos.
  - Nunca exponer una entidad de dominio directo en un controller — la capa web solo conoce DTOs
  - El dominio nunca retorna null como valor valido —  Mono.empty()/Flux.empty() (reactivo), segun el contexto.
  - Un caso de uso = una interfaz (domain/port/in/) + una clase de implementacion (application/) — no consolidar multiples operaciones en una sola interfaz tipo CRUD generico.
  - Usar SpringDoc OpenAPI para Documentación Swagger
  - Usar Sl4j para logs en controladores servicios adaptadores.

## Modelo de datos
Vamos a implementar la aplicación utilizando DynamoDB con Single Table Design. Nombre de la tabla "franchise-management"

1. Estructura general

Utilizaremos una única tabla de DynamoDB.

Las entidades principales serán:

Franchise
Store
Product


PK                    | SK                                  | tipo
FRANCHISE#{id}         | METADATA                            | Franchise
FRANCHISE#{id}         | STORE#{id}                          | Store
FRANCHISE#{id}         | STORE#{id}#PRODUCT#{id}             | Product

La jerarquía lógica del dominio es:

Franchise
   └── Store
         └── Product


 