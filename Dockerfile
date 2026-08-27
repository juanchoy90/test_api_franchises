# Etapa 1: build con Maven (incluye los annotation processors de
# Lombok + MapStruct -- por eso corremos "mvn clean package" completo
# aqui, no solo copiamos el jar de un build local que podria estar
# desactualizado).
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app

# Copiamos primero solo el pom.xml para aprovechar la cache de capas de
# Docker: si no cambian las dependencias, este paso no se repite en
# cada build.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: imagen final, solo el jar + el runtime de Java -- sin Maven,
# sin codigo fuente, sin nada del proceso de build. Amazon Corretto
# porque es la distribucion de OpenJDK que mantiene AWS (coherente con
# que todo el despliegue vive en AWS).
FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
