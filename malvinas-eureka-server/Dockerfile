# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -DskipTests -q

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S malvinas && adduser -S malvinas -G malvinas
USER malvinas

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java",   "-XX:+UseContainerSupport",   "-XX:MaxRAMPercentage=75.0",   "-Djava.security.egd=file:/dev/./urandom",   "-jar", "app.jar"]
