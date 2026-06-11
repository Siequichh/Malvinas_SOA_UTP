# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Malvinas SOA** is a vehicle load and dispatch management system built as a microservices SOA architecture. It consists of 8 Spring Boot services and 1 Angular 19 frontend.

## Build Commands

### Java (Maven) — from project root

```bash
# Build all services
mvn clean install -DskipTests

# Build a single service
mvn clean install -pl malvinas-personal-service -DskipTests

# Run all tests
mvn clean test

# Run tests for a specific service
mvn test -pl malvinas-personal-service

# Run a single test class
mvn test -pl malvinas-personal-service -Dtest=EmployeeServiceTest

# Run a single test method
mvn test -pl malvinas-personal-service -Dtest=EmployeeServiceTest#testCreateEmployee
```

### Frontend (Angular) — from `malvinas-frontend/`

```bash
npm install
npm start              # dev server at http://localhost:4200
npm run build          # development build
npm run build:prod     # production build → dist/malvinas-frontend/browser
npm test               # Karma unit tests
ng lint
```

## Local Development Startup Order

Services must start in this order (each requires Eureka to already be running, except Eureka itself):

1. **Eureka Server** (port 8761) — `cd malvinas-eureka-server && mvn spring-boot:run`
2. **API Gateway** (port 8080) — `cd malvinas-api-gateway && mvn spring-boot:run`
3. Any business service in any order:
   - **personal-service** (8081), **vehiculos-service** (8082), **cargas-service** (8083)
   - **rutas-service** (8084), **reportes-service** (8085), **auth-service** (8086)
4. **Frontend** — `cd malvinas-frontend && npm start`

Each service reads a `.env` file. Copy the `.env.example` in each service directory and fill in values. Defaults for dev work out of the box with a local PostgreSQL on `localhost:5432`.

## Database Setup

Single PostgreSQL instance (`malvinas_db`) with 4 schemas, one per data-owning service:

```sql
CREATE DATABASE malvinas_db;
\c malvinas_db
CREATE SCHEMA IF NOT EXISTS staff;
CREATE SCHEMA IF NOT EXISTS vehicles;
CREATE SCHEMA IF NOT EXISTS loads;
CREATE SCHEMA IF NOT EXISTS routes;
```

Default dev credentials: `postgres / postgres`. `ddl-auto: update` auto-creates tables on first run.

Services with no database: `auth-service` and `reportes-service`.

## Architecture

### Service Map

| Service | Port | DB Schema | Role |
|---|---|---|---|
| malvinas-eureka-server | 8761 | — | Service registry |
| malvinas-api-gateway | 8080 | — | JWT validation, routing |
| malvinas-personal-service | 8081 | `staff` | Employees, roles, attendance |
| malvinas-auth-service | 8086 | — | Login, JWT issuance, refresh tokens |
| malvinas-vehiculos-service | 8082 | `vehicles` | Fleet management |
| malvinas-cargas-service | 8083 | `loads` | Cargo/load management |
| malvinas-rutas-service | 8084 | `routes` | Dispatch, delivery points |
| malvinas-reportes-service | 8085 | — | Dashboard KPIs (aggregates other services) |

### Authentication Flow

1. Frontend POSTs credentials to `/api/auth/**` → Gateway routes this path **without JWT validation**.
2. `auth-service` calls `personal-service` directly (via Eureka `lb://personal-service`) to verify employee by DNI.
3. `auth-service` issues a signed JWT (HMAC-SHA256, secret from `jwt.secret` env var).
4. For all other requests, the Gateway's `JwtAuthFilter` validates the JWT and injects three headers:
   - `X-Employee-Id`, `X-Employee-Role`, `X-Employee-Name`
5. Each downstream service reads these headers via `RequestHeaderAuthFilter` (extends `OncePerRequestFilter`) and builds a `SecurityContext` — **no JWT re-validation in downstream services**.

### Roles

`ADM` (admin), `SUP` (supervisor), `MOV` (mobilizador), `DRV` (driver), `SEC` (security). Used with Spring Security `@PreAuthorize` and `hasRole()` rules.

### Gateway Routing

Defined in `GatewayRoutesConfig.java`. Dev profile uses direct `http://localhost:PORT` URLs; prod profile uses Eureka load-balanced `lb://service-name` URIs. The gateway uses **Spring Cloud Gateway MVC** (servlet-based, not WebFlux).

### Internal Package Structure (per business service)

```
domain/
  entity/          # JPA entities extending AuditableEntity
  repository/      # Spring Data JPA repositories
  service/         # Business logic interfaces + impls
application/
  dto/             # Request/Response DTOs
infrastructure/
  mapper/          # MapStruct mappers (entity ↔ DTO)
  exception/       # Custom exceptions + global handler
  security/        # RequestHeaderAuthFilter
config/            # SecurityConfig, OpenAPI config, RestClient beans
```

`AuditableEntity` (MappedSuperclass) provides `createdAt`, `modifiedAt`, `createdBy`, `modifiedBy` on all entities.

Enums implement `DisplayableEnum` interface (code, displayName, description, available).

### Cross-Service Calls

`cargas-service` calls `vehiculos-service` to validate vehicles. `rutas-service` calls both `vehiculos-service` and `personal-service`. `reportes-service` calls three services to aggregate KPIs. All use `@LoadBalanced RestClient` via Eureka. Each cross-service caller has a **Resilience4j circuit breaker** (sliding window 5, 50% failure threshold, 10s open wait).

### Frontend

Angular 19 standalone components (no NgModules). State via Signals. All API calls go through the Gateway at `environment.apiUrl`. A `jwtInterceptor` attaches the Bearer token to every request. Route guard (`authGuard`) protects all routes except `/login`. Production `apiUrl` is set to `https://malvinas-api-gateway.onrender.com` in `environment.prod.ts`.

## Environment Variables

Each service has a `.env.example`. Key variables shared across services:

| Variable | Used by |
|---|---|
| `ENVIRONMENT` | Profile selection (`dev`/`prod`) |
| `JWT_SECRET` | gateway, personal-service, auth-service |
| `EUREKA_URI` | All services except eureka-server |
| `LOCAL_DB_HOST/NAME/USERNAME/PASSWORD` | DB services (dev) |
| `POSTGRESQL_HOST/DB/USER/PASS` | DB services (prod/Render) |
| `JWT_ACCESS_EXPIRATION` / `JWT_REFRESH_EXPIRATION` | auth-service |

## Swagger UI

Each service exposes Swagger at `http://localhost:<PORT>/swagger-ui.html` (dev only, disabled in prod via SpringDoc config).
