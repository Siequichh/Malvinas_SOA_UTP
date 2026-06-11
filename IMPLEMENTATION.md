# Grupo Malvina SOA — Documentacion de Implementacion

Sistema de Control de Carga y Despacho Vehicular. Arquitectura SOA con 6 microservicios de negocio + 2 de infraestructura + frontend Angular 19.

---

## Estructura del Proyecto

```
malvinas_SOA/
├── pom.xml                          # Reactor POM raiz (abre en IntelliJ para reconocer todos los modulos)
├── .gitignore
├── IMPLEMENTATION.md
├── malvinas-eureka-server/          # Service Discovery (puerto 8761)
├── malvinas-api-gateway/            # API Gateway + JWT filter (puerto 8080)
├── malvinas-personal-service/       # Gestion de empleados/roles/asistencia (puerto 8081)
├── malvinas-auth-service/           # Autenticacion JWT sin BD (puerto 8086)
├── malvinas-vehiculos-service/      # Gestion de flota vehicular (puerto 8082)
├── malvinas-cargas-service/         # Control del proceso de carga (puerto 8083)
├── malvinas-rutas-service/          # Despacho y rutas (puerto 8084)
├── malvinas-reportes-service/       # Dashboard y KPIs - sin BD (puerto 8085)
└── malvinas-frontend/               # Angular 19 + PrimeNG (puerto 4200)
```

---

## Stack Tecnologico

### Backend
| Tecnologia | Version | Uso |
|---|---|---|
| Java | 21 | Runtime (LTS) |
| Spring Boot | 4.0.6 | Framework base |
| Spring Cloud | 2025.1.1 (Oakwood) | Eureka, Gateway, LoadBalancer |
| Spring Data JPA | (Spring Boot) | ORM con PostgreSQL |
| Spring Security | (Spring Boot) | JWT filter chain + RequestHeaderAuthFilter |
| Lombok | (Spring Boot) | Reduccion de boilerplate en entidades |
| MapStruct | 1.6.3 | Entity <-> DTO mapping |
| auth0 java-jwt | 4.4.0 | JWT HMAC-SHA256 (auth-service + api-gateway) |
| Resilience4j | 2.4.0 (`resilience4j-spring-boot4`) | CircuitBreaker inter-servicio |
| SpringDoc OpenAPI | 3.0.2 | Swagger UI en `/swagger-ui.html` |
| PostgreSQL | 17 | Base de datos (4 schemas) |

### Frontend
| Tecnologia | Version | Uso |
|---|---|---|
| Angular | 19 | Framework SPA (standalone components, signals) |
| PrimeNG | 19 + Aura theme | Componentes UI |
| PrimeFlex | 3.3 | Utilidades CSS |
| Tailwind CSS | 3.x | Utilidades (preflight: false para no conflictar con PrimeNG) |
| TypeScript | 5.6 | Lenguaje |

---

## Patrones Implementados

### Entidades
- `AuditableEntity` (`@MappedSuperclass`) con `createdAt`, `modifiedAt`, `createdBy`, `modifiedBy` via Spring Data JPA Auditing
- Lombok en todas las entidades: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder`

### Enums
- Todos implementan `DisplayableEnum` con 4 campos: `code`, `displayName`, `description`, `available`
- JPA `AttributeConverter` para persistir enums como `VARCHAR(2)` en BD

### DTOs
- Todos como Java Records con anotaciones de validacion `@NotBlank`, `@Size`, etc.

### MapStruct
- `componentModel = "spring"` configurado en `maven-compiler-plugin`
- Mapper por servicio, con `@Mapper(componentModel = "spring")`

### Seguridad
- **API Gateway**: Valida JWT con `auth0/java-jwt`, agrega headers `X-Employee-Id`, `X-Employee-Role`, `X-Employee-Name`
- **Servicios internos**: `RequestHeaderAuthFilter` (OncePerRequestFilter) lee esos headers y crea `SecurityContext`
- **SecurityConfig**: `@EnableWebSecurity @EnableMethodSecurity`, reglas por rol (`ROLE_ADM`, `ROLE_SUP`, etc.)
- **personal-service**: Endpoints internos (`by-dni`, `active`) son `permitAll` para que auth-service los consuma sin JWT

### Inter-servicio
- `RestClient` con `@LoadBalanced` via Eureka (URL: `http://nombre-del-servicio`)
- Resilience4j `@CircuitBreaker` en llamadas criticas

### ApiPaths
- Clase `ApiPaths.java` con constantes de todos los paths REST en `infrastructure/controller/`

---

## Arquitectura de Base de Datos

Una sola instancia PostgreSQL con 4 schemas. `ddl-auto: update` en desarrollo.

| Schema | Servicio | Tablas principales |
|---|---|---|
| `staff` | personal-service | `roles`, `employees`, `attendances`, `refresh_tokens` |
| `vehicles` | vehiculos-service | `vehicle_types`, `vehicles`, `status_history` |
| `loads` | cargas-service | `loads`, `load_details` |
| `routes` | rutas-service | `delivery_points`, `dispatches`, `dispatch_points`, `reloads` |

---

## Flujo de Seguridad

```
Cliente (Angular)
    │
    ▼
POST /api/auth/login
    │
    ▼
API Gateway (sin JwtAuthFilter para /api/auth/**)
    │
    ▼
auth-service
    │── llama personal-service /api/employees/by-dni/{dni}  (sin JWT)
    │── valida credenciales
    └── retorna { accessToken, refreshToken, employee }
    │
    ▼
Cliente almacena JWT en localStorage

Siguientes requests con Authorization: Bearer <token>
    │
    ▼
API Gateway (JwtAuthFilter)
    │── valida JWT con JWT_SECRET
    │── agrega headers: X-Employee-Id, X-Employee-Role, X-Employee-Name
    └── reenvía al microservicio correspondiente
         │
         ▼
    Servicio interno (RequestHeaderAuthFilter)
         │── lee X-Employee-Id + X-Employee-Role
         │── crea SecurityContext con ROLE_XXX
         └── SecurityConfig aplica reglas por rol
```

---

## Variables de Entorno

Cada servicio tiene `.env` (local) y `.env.example` (template para deploy). Copiar `.env.example` a `.env` y llenar con valores reales.

### Variables compartidas entre servicios

| Variable | Descripcion | Ejemplo local | Ejemplo Render |
|---|---|---|---|
| `JWT_SECRET` | Clave HMAC-SHA256 (minimo 32 chars) | `malvinas-dev-secret-...` | Generar con `openssl rand -base64 32` |
| `EUREKA_URI` | URL del servidor Eureka | `http://localhost:8761/eureka` | `https://malvinas-eureka.onrender.com/eureka` |

### Variables de base de datos en .env local (personal, vehiculos, cargas, rutas)

| Variable | Descripcion | Valor por defecto |
|---|---|---|
| `LOCAL_DB_HOST` | Host PostgreSQL | `localhost` |
| `LOCAL_DB_NAME` | Nombre de la BD | `malvinas_db` |
| `LOCAL_DB_USERNAME` | Usuario | `postgres` |
| `LOCAL_DB_PASSWORD` | Contrasena | `postgres` |

> El puerto 5432 esta hardcodeado en `application-dev.yml`. DDL-auto (`update`) tambien esta hardcodeado por perfil — no es una variable de entorno.

### Variables de base de datos en Render (prod)

| Variable | Descripcion |
|---|---|
| `POSTGRESQL_HOST` | Render PostgreSQL host (de la dashboard de Render) |
| `POSTGRESQL_DB` | Nombre de la BD |
| `POSTGRESQL_USER` | Usuario |
| `POSTGRESQL_PASS` | Contrasena |

---

## Ejecucion Local (paso a paso)

### Prerequisitos
- Java 21 (JDK)
- Maven 3.9+
- PostgreSQL 17 corriendo en localhost:5432
- Node.js 20+ y npm (para frontend)

### 1. Preparar base de datos

```sql
-- Crear base de datos
CREATE DATABASE malvinas_db;

-- Crear schemas (conectarse a malvinas_db primero)
\c malvinas_db
CREATE SCHEMA IF NOT EXISTS staff;
CREATE SCHEMA IF NOT EXISTS vehicles;
CREATE SCHEMA IF NOT EXISTS loads;
CREATE SCHEMA IF NOT EXISTS routes;
```

### 2. Configurar variables de entorno

Cada servicio tiene un archivo `.env` con valores por defecto para localhost. Si PostgreSQL usa usuario/clave distintos, editar los `.env` de cada servicio.

### 3. Abrir el proyecto en IntelliJ IDEA

**Importante para el error "Java file outside module source root":**
1. Abrir IntelliJ IDEA
2. File → Open → seleccionar `C:\projects\malvinas_SOA\pom.xml` (el reactor POM raiz)
3. Seleccionar "Open as Project"
4. IntelliJ importara todos los modulos automaticamente y reconocera los source roots

Si ya tienes el proyecto abierto y ves el error:
- Panel Maven (derecha) → boton "+" → agregar `malvinas-api-gateway\pom.xml` (y repetir para cada servicio)
- O: clic derecho en el pom.xml del servicio → "Add as Maven Project"

### 4. Iniciar servicios en orden

Iniciar en este orden (cada uno en terminal separada o desde IntelliJ):

```bash
# 1. Eureka Server
cd malvinas-eureka-server
mvn spring-boot:run

# 2. API Gateway (esperar que Eureka este listo)
cd malvinas-api-gateway
mvn spring-boot:run

# 3. personal-service (esperar que Gateway este listo)
cd malvinas-personal-service
mvn spring-boot:run

# 4. auth-service
cd malvinas-auth-service
mvn spring-boot:run

# 5. vehiculos-service
cd malvinas-vehiculos-service
mvn spring-boot:run

# 6. cargas-service
cd malvinas-cargas-service
mvn spring-boot:run

# 7. rutas-service
cd malvinas-rutas-service
mvn spring-boot:run

# 8. reportes-service
cd malvinas-reportes-service
mvn spring-boot:run
```

O con variables de entorno explicitamente:

```bash
# Ejemplo para personal-service en Windows PowerShell
$env:DB_PASSWORD="mi_password"; mvn spring-boot:run
```

### 5. Iniciar frontend

```bash
cd malvinas-frontend
npm install
ng serve
# Disponible en http://localhost:4200
```

### 6. Verificar funcionamiento

| Servicio | URL | Descripcion |
|---|---|---|
| Eureka dashboard | http://localhost:8761 | Ver servicios registrados |
| API Gateway | http://localhost:8080 | Punto de entrada unico |
| personal-service Swagger | http://localhost:8081/swagger-ui.html | Docs API |
| auth-service Swagger | http://localhost:8086/swagger-ui.html | Docs auth |
| vehiculos-service Swagger | http://localhost:8082/swagger-ui.html | Docs vehiculos |
| cargas-service Swagger | http://localhost:8083/swagger-ui.html | Docs cargas |
| rutas-service Swagger | http://localhost:8084/swagger-ui.html | Docs rutas |
| Frontend Angular | http://localhost:4200 | UI del sistema |

### Test rapido con curl

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"dni":"12345678","password":"admin123"}'

# Listar vehiculos (reemplazar TOKEN con el accessToken del login)
curl http://localhost:8080/api/vehicles \
  -H "Authorization: Bearer TOKEN"
```

---

## Deploy en Render (paso a paso)

### Prerequisitos en Render
- Cuenta en render.com
- Plan gratuito permite: 1 PostgreSQL, hasta 6 Web Services Docker

### 1. Crear PostgreSQL en Render

1. Dashboard → New → PostgreSQL
2. Name: `malvinas-db`
3. Database: `malvinas_db`, User: `malvinas_user`
4. Guardar los datos de conexion (Host, Port, Database, Username, Password)
5. Conectarse y crear los 4 schemas:
   ```sql
   CREATE SCHEMA IF NOT EXISTS staff;
   CREATE SCHEMA IF NOT EXISTS vehicles;
   CREATE SCHEMA IF NOT EXISTS loads;
   CREATE SCHEMA IF NOT EXISTS routes;
   ```

### 2. Generar JWT Secret

```bash
openssl rand -base64 32
# Guardar el resultado, se usa en todos los servicios
```

### 3. Subir codigo a GitHub

```bash
cd C:\projects\malvinas_SOA
git init
git add .
git commit -m "Initial commit - Malvinas SOA"
git remote add origin https://github.com/tu-usuario/malvinas-soa.git
git push -u origin main
```

### 4. Deploy eureka-server

1. Render → New → Web Service
2. Conectar repositorio GitHub, Root Directory: `malvinas-eureka-server`
3. Environment: Docker
4. Variables de entorno:
   - `SERVER_PORT` = `10000` (Render asigna el puerto externo)
   - `EUREKA_HOSTNAME` = `<url-del-servicio>.onrender.com`
5. Deploy. Copiar la URL resultante (ej: `https://malvinas-eureka.onrender.com`)

### 5. Deploy de cada microservicio

Repetir para cada servicio en este orden: `personal-service`, `auth-service`, `vehiculos-service`, `cargas-service`, `rutas-service`, `reportes-service`, `api-gateway`

Variables comunes para cada servicio:
```
ENVIRONMENT=prod
SERVER_PORT=10000
EUREKA_URI=https://malvinas-eureka.onrender.com
JWT_SECRET=<el secret generado en paso 2>
```

> **IMPORTANTE:** `EUREKA_URI` es la URL base **sin** `/eureka` al final. El sufijo `/eureka` lo agrega `application-prod.yml` internamente.

Variables DB (para servicios con base de datos: personal, vehiculos, cargas, rutas):
```
POSTGRESQL_HOST=<Render PostgreSQL host>
POSTGRESQL_DB=malvinas_db
POSTGRESQL_USER=malvinas_user
POSTGRESQL_PASS=<Render PostgreSQL password>
```

### 6. Deploy frontend

1. Render → New → Static Site
2. Root Directory: `malvinas-frontend`
3. Build Command: `npm install && npm run build`
4. Publish Directory: `dist/malvinas-frontend/browser`
5. Variables de entorno:
   - `API_URL` = URL del api-gateway en Render

### 7. Configurar UptimeRobot (evitar cold starts en free tier)

1. Crear cuenta en uptimerobot.com
2. Para cada servicio, crear monitor HTTP:
   - URL: `https://<servicio>.onrender.com/actuator/health`
   - Intervalo: 5 minutos
3. Esto evita que los servicios "duerman" en el plan gratuito

### Orden de URLs inter-servicio en Render

En Render, los servicios se comunican via Eureka usando su nombre de aplicacion (`spring.application.name`). La URL `lb://personal-service` funciona igual que en local — Eureka resuelve el nombre al IP interno del contenedor.

**No es necesario hardcodear URLs de Render en el codigo.** El patron `lb://service-name` funciona en ambos entornos.

---

## Estructura interna de cada servicio

```
src/main/java/com/malvinas/{service}/
├── application/
│   └── dto/                    # Java Records (Request + Response)
├── domain/
│   ├── entity/                 # JPA Entities (Lombok: @Getter @Setter @SuperBuilder)
│   ├── enumerate/              # Enums implementando DisplayableEnum
│   ├── audit/                  # AuditableEntity (@MappedSuperclass)
│   ├── repository/             # Spring Data JPA Repositories
│   └── service/                # Interfaces + impl/
├── infrastructure/
│   ├── controller/             # @RestController + ApiPaths.java
│   ├── mapper/                 # MapStruct Mappers
│   ├── exception/              # GlobalExceptionHandler, BusinessException, ResourceNotFoundException
│   ├── security/               # RequestHeaderAuthFilter
│   └── util/converter/         # JPA AttributeConverters para enums
└── config/                     # SecurityConfig, AuditConfig, OpenApiConfig, etc.
```

---

## Roles del Sistema

| Codigo | Nombre | Acceso |
|---|---|---|
| `ADM` | Administrador | Acceso completo |
| `SUP` | Supervisor | Gestiona despachos, ve flota |
| `MOV` | Movilizador | Gestiona cargas |
| `DRV` | Conductor | Ve sus despachos, registra salida/llegada |
| `SEC` | Seguridad | Valida ordenes de carga en porteria |

---

## Estados y Transiciones

### Vehiculo (VehicleStatus)
```
AVAILABLE → LOADING → LOADED → ON_ROUTE → AVAILABLE
AVAILABLE → MAINTENANCE → AVAILABLE
LOADING   → AVAILABLE (cancelacion de carga)
LOADED    → AVAILABLE (cancelacion antes de salir)
```

### Carga (LoadStatus)
```
PENDING → IN_PROGRESS → COMPLETED
PENDING → CANCELLED
IN_PROGRESS → CANCELLED
```

### Despacho (DispatchStatus en rutas-service)
```
SCHEDULED → ON_ROUTE → COMPLETED
SCHEDULED → CANCELLED
```

---

## Notas de Desarrollo

### Spring Cloud Gateway — MVC vs WebFlux
- El proyecto usa **`spring-cloud-starter-gateway-server-webmvc`** (Spring Cloud 2025.1.1 / Gateway 5.0.1)
- Este es el modo **MVC (Servlet)**, no WebFlux/Reactor — compatible con JPA y RestClient en el mismo classpath
- El artifact `spring-cloud-starter-gateway` y `spring-cloud-starter-gateway-mvc` **ya no existen** en Gateway 5.x. Los nombres correctos son:
  - MVC (servlet): `spring-cloud-starter-gateway-server-webmvc`
  - WebFlux (reactivo): `spring-cloud-starter-gateway-server-webflux`
- El filtro JWT es un `jakarta.servlet.Filter` con `@Order(1)`, NO un `AbstractGatewayFilterFactory`
- Las rutas se configuran en `spring.cloud.gateway.mvc.routes` (no `spring.cloud.gateway.routes`)
- **Servicios de negocio** (personal, vehiculos, cargas, rutas, auth, reportes): Spring MVC (servlet) — identico al gateway

### Inter-servicio local vs deploy
- Comunicacion via `lb://nombre-servicio` (Eureka load-balancer) funciona igual en local y Render
- Ejemplo: `http://malvinas-vehiculos-service` se resuelve via Eureka en ambos entornos
- Variables de entorno con valores por defecto: `${EUREKA_URI:http://localhost:8761/eureka}`

### Patron ApiPaths
Todos los controllers usan constantes de `ApiPaths.java` en lugar de strings literales:
```java
// ApiPaths.java (en el mismo paquete que los controllers)
public static final String EMPLOYEES = "/api/employees";

// EmployeeController.java
@RestController
@RequestMapping(ApiPaths.EMPLOYEES)   // ← constante, no string literal
@RequiredArgsConstructor
public class EmployeeController { ... }
```
Esto centraliza los paths y evita errores de tipeo al refactorizar.

### Eureka Server — Spring Security
`spring-cloud-starter-netflix-eureka-server` incluye Spring Security transitivamente.
La solucion correcta es un `SecurityConfig` que permite todo el trafico (servicio interno):
```java
// malvinas-eureka-server/config/SecurityConfig.java
@Configuration @EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```
No usar `spring.autoconfigure.exclude` — genera advertencias en IDE y es fragil.

---

## Problemas comunes (IDE y Frontend)

### IntelliJ: "Java file is located outside of the module source root"
Ocurre cuando IntelliJ no reconoce el proyecto como multi-modulo Maven.

**Solucion:**
1. Cerrar IntelliJ completamente
2. File → Open → seleccionar `C:\projects\malvinas_SOA\pom.xml` (el reactor POM raiz)
3. Seleccionar **"Open as Project"** (NO "Add to existing project")
4. IntelliJ importara todos los modulos y reconocera los source roots automaticamente

Si el error persiste en un modulo especifico:
- Panel Maven (derecha) → `+` → agregar el `pom.xml` del modulo afectado
- O: clic derecho en `src/main/java` del modulo → Mark Directory as → Sources Root

### Frontend: "module 'tslib' cannot be found"
Causado por `"importHelpers": true` en `tsconfig.json` cuando tslib no esta instalado.

**Solucion aplicada:** `"importHelpers": false` en `tsconfig.json` — elimina la dependencia de tslib.

**Alternativa:** ejecutar `npm install` en `malvinas-frontend/` (tslib esta en `package.json`).

### Frontend: "Could not resolve 'chart.js/auto'"

PrimeNG `ChartModule` necesita `chart.js` como peer dependency — no se instala automaticamente.

**Solucion:**
```bash
npm install chart.js --prefix malvinas-frontend
```

### Frontend: Tailwind no aplica estilos / "postcss: command not found"

`postcss.config.js` debe existir en la raiz de `malvinas-frontend/` para que Angular procese Tailwind.

**Verificar que existe:**
```js
// malvinas-frontend/postcss.config.js
module.exports = { plugins: { tailwindcss: {}, autoprefixer: {} } };
```

### Frontend: PrimeNG Button `severity="warning"` — error de tipo

En PrimeNG 19, `ButtonSeverity` usa `"warn"` (no `"warning"`). En `p-tag` si se acepta `"warning"`.

```html
<!-- Incorrecto -->  <p-button severity="warning" />
<!-- Correcto -->    <p-button severity="warn" />
```

### Frontend: `p-card style="..."` — Type error

En PrimeNG 19, `p-card` tiene un input `style` tipado como `NgStyle` object. Usar binding:

```html
<!-- Incorrecto -->  <p-card style="max-width: 400px">
<!-- Correcto -->    <p-card [style]="{'max-width': '400px'}">
```

### Frontend: estructura de 3 archivos por componente
Cada componente Angular usa 3 archivos separados (nunca `template:` inline):
```
cargas.component.ts      ← logica + @Component con templateUrl/styleUrls
cargas.component.html    ← template HTML
cargas.component.scss    ← estilos (:host { display: block; } minimo)
```

---

## Pendiente para produccion
- Migrar de `ddl-auto: update` a Flyway migrations (`V1__init.sql` por servicio)
- HTTPS: Render lo gestiona automaticamente en todos los Web Services
- Rate limiting en API Gateway (Spring Cloud Gateway RateLimiter)
- Configurar `JPA_DDL_AUTO=validate` en produccion (nunca `update` o `create`)
