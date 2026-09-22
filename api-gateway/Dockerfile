# Multi-stage build: compile with Maven, ship only the runtime jar.
# Used for deployment (Railway/Render/any Docker host) - local development
# still uses "mvn spring-boot:run" directly (see the root README / .bat files).

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S quickbite && adduser -S quickbite -G quickbite
COPY --from=build /workspace/target/*.jar app.jar
USER quickbite
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
