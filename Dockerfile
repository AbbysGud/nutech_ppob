FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml ./

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests \
        -Dmaven.wagon.http.retryHandler.count=5 \
        -Dmaven.wagon.http.pool=false \
        dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests \
        -Dmaven.wagon.http.retryHandler.count=5 \
        -Dmaven.wagon.http.pool=false \
        package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/target/*.jar /app/app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
CMD ["java", "-jar", "/app/app.jar"]
