FROM maven:3.9.14-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/ronda-backend-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/uploads && chown -R 10001:10001 /app
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
