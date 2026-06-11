# Sistema de Gestión de Cargas y Despacho — Malvinas SOA

Proyecto desarrollado para el curso de **Arquitectura Orientada a Servicios (SOA)**
**Universidad Tecnológica del Perú (UTP)**

---

## Descripción

Sistema de gestión operativa para el control de cargas y despacho de vehículos. Permite administrar el personal, la flota vehicular, las cargas asignadas, las rutas de despacho y generar reportes en tiempo real mediante un tablero de indicadores.

El sistema aplica los principios de la **Arquitectura Orientada a Servicios (SOA)**: cada capacidad del negocio está expuesta como un servicio independiente con contrato bien definido, comunicación vía HTTP/REST y descubrimiento de servicios centralizado.

---

## Arquitectura

El sistema está compuesto por **8 microservicios Spring Boot** y **1 frontend Angular 19**, todos coordinados por un API Gateway y un servidor de descubrimiento Eureka.

```
┌─────────────────────────────────────────────────────────────┐
│                    FRONTEND (Angular 19)                     │
│                     Puerto 4200                              │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP (JWT)
┌──────────────────────────▼──────────────────────────────────┐
│                   API GATEWAY                                │
│         Puerto 8080 — Validación JWT + Enrutamiento          │
└──────┬──────────┬────────┬──────────┬──────────┬────────────┘
       │          │        │          │          │
  ┌────▼───┐ ┌───▼────┐ ┌─▼──────┐ ┌─▼──────┐ ┌▼────────┐
  │  Auth  │ │Personal│ │Vehíc.  │ │Cargas  │ │  Rutas  │
  │  8086  │ │  8081  │ │  8082  │ │  8083  │ │  8084   │
  └────────┘ └───┬────┘ └─┬──────┘ └────────┘ └─────────┘
                 │        │
            ┌────▼────────▼───────────────┐
            │   PostgreSQL — malvinas_db   │
            │  schemas: staff | vehicles   │
            │          loads | routes      │
            └─────────────────────────────┘

┌────────────────────────┐   ┌────────────────────────────┐
│   Reportes  (8085)     │   │   Eureka Server  (8761)    │
│   KPIs y Dashboard     │   │   Descubrimiento           │
└────────────────────────┘   └────────────────────────────┘
```

### Servicios

| Servicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| `malvinas-eureka-server` | 8761 | — | Registro y descubrimiento de servicios |
| `malvinas-api-gateway` | 8080 | — | Punto de entrada único, validación JWT, enrutamiento |
| `malvinas-personal-service` | 8081 | schema `staff` | Gestión de empleados, roles y asistencia |
| `malvinas-auth-service` | 8086 | — | Autenticación, emisión y renovación de tokens JWT |
| `malvinas-vehiculos-service` | 8082 | schema `vehicles` | Gestión de flota y tipos de vehículo |
| `malvinas-cargas-service` | 8083 | schema `loads` | Control de cargas y sus detalles |
| `malvinas-rutas-service` | 8084 | schema `routes` | Despachos, puntos de entrega y recarga |
| `malvinas-reportes-service` | 8085 | — | Dashboard e indicadores KPI agregados |

---

## Tecnologías Utilizadas

### Backend
- **Java 21** + **Spring Boot 4.0.6**
- **Spring Cloud 2025.1.1 (Oakwood)** — Eureka, Gateway MVC
- **Spring Security** + **auth0 java-jwt 4.4.0** — autenticación HMAC-SHA256
- **Spring Data JPA** + **PostgreSQL 17**
- **MapStruct 1.6.3** — mapeo entidad ↔ DTO
- **Resilience4j 2.4.0** — circuit breaker entre servicios
- **SpringDoc OpenAPI 3.0.2** — documentación Swagger
- **Lombok** — reducción de boilerplate

### Frontend
- **Angular 19** con componentes standalone y Signals
- **PrimeNG 19** + tema Aura — componentes UI
- **Tailwind CSS 3** + PrimeFlex — estilos utilitarios
- **Chart.js 4.5** — gráficos en el dashboard
- **RxJS 7.8** — programación reactiva

### Base de Datos
- **PostgreSQL 17** — una instancia con 4 schemas (uno por servicio con datos)

---

## Flujo de Autenticación

1. El usuario ingresa credenciales en el frontend → `POST /api/auth/login`
2. El Gateway reenvía la petición al **auth-service** sin validar JWT (ruta pública)
3. El auth-service consulta al **personal-service** para verificar al empleado por DNI
4. Si las credenciales son correctas, emite un JWT firmado (acceso: 1 hora, refresco: 7 días)
5. En cada petición subsiguiente, el Gateway valida el JWT y extrae: `X-Employee-Id`, `X-Employee-Role`, `X-Employee-Name`
6. Los servicios downstream leen esos headers para construir el contexto de seguridad (sin re-validar el JWT)

### Roles del sistema

| Rol | Código | Permisos principales |
|---|---|---|
| Administrador | `ADM` | Acceso total |
| Supervisor | `SUP` | Consultas y operaciones de supervisión |
| Mobilizador | `MOV` | Gestión de cargas |
| Conductor | `DRV` | Consulta de sus despachos |
| Seguridad | `SEC` | Consulta de personal y vehículos |

---

## Requisitos Previos

- **Java 21** o superior
- **Maven 3.9+**
- **Node.js 20+** y **npm**
- **PostgreSQL 17** corriendo en `localhost:5432`
- **Angular CLI** (`npm install -g @angular/cli`)

---

## Configuración Local

### 1. Base de Datos

```sql
CREATE DATABASE malvinas_db;
\c malvinas_db
CREATE SCHEMA IF NOT EXISTS staff;
CREATE SCHEMA IF NOT EXISTS vehicles;
CREATE SCHEMA IF NOT EXISTS loads;
CREATE SCHEMA IF NOT EXISTS routes;
```

### 2. Variables de Entorno

Cada servicio tiene un archivo `.env.example` en su directorio. Copia cada uno como `.env`:

```bash
# Ejemplo para personal-service
ENVIRONMENT=dev
LOCAL_DB_HOST=localhost
LOCAL_DB_NAME=malvinas_db
LOCAL_DB_USERNAME=postgres
LOCAL_DB_PASSWORD=postgres
JWT_SECRET=malvinas-dev-secret
EUREKA_URI=http://localhost:8761
```

### 3. Arranque de Servicios (en orden)

```bash
# 1. Eureka (esperar a que inicie antes de arrancar los demás)
cd malvinas-eureka-server && mvn spring-boot:run

# 2. API Gateway
cd malvinas-api-gateway && mvn spring-boot:run

# 3. Servicios de negocio (cualquier orden)
cd malvinas-personal-service && mvn spring-boot:run
cd malvinas-auth-service && mvn spring-boot:run
cd malvinas-vehiculos-service && mvn spring-boot:run
cd malvinas-cargas-service && mvn spring-boot:run
cd malvinas-rutas-service && mvn spring-boot:run
cd malvinas-reportes-service && mvn spring-boot:run

# 4. Frontend
cd malvinas-frontend && npm install && npm start
```

La aplicación queda disponible en `http://localhost:4200`.
Eureka Dashboard: `http://localhost:8761`.

---

## Patrones SOA Aplicados

- **Service Registry & Discovery** — Eureka Server centraliza el registro de todos los servicios
- **API Gateway** — punto de entrada único con validación de seguridad transversal
- **Circuit Breaker** — Resilience4j previene fallos en cascada entre servicios
- **Stateless Authentication** — JWT sin estado de sesión en el servidor
- **Schema-per-Service** — cada servicio de datos posee su propio schema en PostgreSQL (aislamiento lógico)
- **Header Propagation** — el contexto del usuario se propaga mediante headers HTTP entre servicios
- **Loose Coupling** — los servicios se comunican por contrato REST, sin dependencias directas de código

---

## Documentación Adicional

- [`IMPLEMENTATION.md`](./IMPLEMENTATION.md) — Guía detallada de implementación, patrones y decisiones de diseño
- [`WEBFLUX_MIGRATION.md`](./WEBFLUX_MIGRATION.md) — Plan de migración a Spring WebFlux (programación reactiva)
- Swagger UI de cada servicio disponible en `http://localhost:<PUERTO>/swagger-ui.html` (solo en perfil `dev`)
