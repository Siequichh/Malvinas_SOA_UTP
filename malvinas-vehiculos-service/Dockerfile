# ── Etapa 1: Compilacion ────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
RUN apk add --no-cache maven
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# ── Etapa 2: Ejecucion ───────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios por seguridad
RUN addgroup -S malvinas && adduser -S malvinas -G malvinas
USER malvinas

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
