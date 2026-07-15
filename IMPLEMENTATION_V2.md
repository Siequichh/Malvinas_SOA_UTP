# Grupo Malvina SOA — Documentación de Implementación V2

> Sucesor de `IMPLEMENTATION.md` (V1, mayo). Este documento refleja el estado **actual** del proyecto
> tras el deploy en Render y la tanda de fixes de producción acumulados en la rama `dev`.
> Para detalles de arquitectura base (patrones, estructura de paquetes, stack) que no cambiaron, ver `IMPLEMENTATION.md`.

Sistema de Control de Carga y Despacho Vehicular. Arquitectura SOA: 6 microservicios de negocio +
2 de infraestructura (Eureka + Gateway) + frontend Angular 19. Desplegado en Render (free tier).

---

## 1. Changelog V1 → V2 (rama `dev`)

Seis commits sobre el `first commit` llevaron el proyecto de "construido localmente" a "desplegado y endurecido":

| Commit | Tipo | Qué aportó |
|---|---|---|
| `8cf0d61` | feat | **Deploy-ready**: UI polish (login refactor, search bars, striped rows, confirm dialogs), auto-refresh 30s, hash routing, Dockerfiles, DataInitializers, `render.yaml` blueprint, `DEPLOY_RENDER.md`, `FLUJO_TRABAJO.md` |
| `27f4556` | fix | `render.yaml` corregido para que el deploy funcione |
| `e7d4276` | fix | Gateway routing `lb://` arreglado (raíz del 500 en login) |
| `05c19d5` | fix | reportes-service: `AggregatorClient` con cliente `@LoadBalanced` (raíz del 500 en dashboard) |
| `1b840da` | fix | Remember-me: flujo de recordar DNI + contraseña con almacenamiento encriptado |
| `7c3296b` | fix | Mensajes de error amigables, validación frontend, loading/disabled en todos los botones, limpieza de `DNI_KEY` que rompía el build |

---

## 2. Frontend — Lo nuevo en V2

### 2.1 Remember-me encriptado (`core/utils/credential-store.ts`)
Guarda DNI + contraseña en `localStorage` **cifrados con AES-GCM 256-bit**, con clave derivada vía
**PBKDF2** (100 000 iteraciones, SHA-256) usando la Web Crypto API nativa del navegador — sin librerías.
- IV aleatorio por cada guardado → el mismo input produce distinto ciphertext.
- Si el blob se corrompe/manipula, el `decrypt` falla y se borra solo (`try/catch`).
- `login.component.ts` llama `saveCredentials()` / `clearCredentials()`; al re-entrar, `ngOnInit` precarga
  los campos. Los tokens van a `localStorage` (con remember) o `sessionStorage` (sin remember).

### 2.2 Hash routing (`withHashLocation`)
Las URLs ahora son `…/#/dashboard`. Elimina el **404 en hosting estático** al recargar o entrar directo
a una ruta — el servidor solo sirve `index.html` y Angular Router resuelve el fragmento. Complementa la
regla de rewrite del `render.yaml`.

### 2.3 Auto-refresh 30s (rutas, cargas, dashboard)
`setInterval(() => this.loadX(), 30_000)` en `ngOnInit` + `clearInterval` en `ngOnDestroy`. Da la
"visibilidad en tiempo real" del TO-BE: el SUP ve cargas completadas por el MOV sin recargar.

### 2.4 Manejo de errores y UX de botones
- `core/utils/error.utils.ts`: `parseApiError()` / `parseLoginError()` traducen status HTTP a mensajes
  en español. El status 400 ya **no** filtra el mensaje crudo de Spring (`mobilizerId: no debe ser nulo…`).
- **Validación frontend** en cada `save*()`: verifica campos requeridos antes de tocar la API y muestra
  un toast amigable.
- **`submitting` signal** en los 4 componentes: botones de submit con `[loading]` + `[disabled]`, y
  botones de acción en tabla deshabilitados mientras hay un request en vuelo → **evita doble-submit**.

### 2.5 Otros
Search bar global (`p-iconfield`) en las 4 tablas, striped rows, orden por relevancia operacional
(estados activos primero), confirm dialogs antes de acciones destructivas (`ConfirmationService`).

---

## 3. Backend / Infra — Lo nuevo en V2

### 3.1 Gateway `lb://` fix (`GatewayRoutesConfig.java`)
Spring Cloud Gateway **Server MVC** (servlet) no resuelve URIs `lb://` con `BeforeFilterFunctions.uri()`
— el Apache HttpClient rechaza el esquema (`Unroutable protocol scheme: lb://`). Solución: helper `proxy()`
que usa `LoadBalancerFilterFunctions.lb(serviceName)` para URIs `lb://`, resolviendo el servicio vía
Eureka **antes** de la llamada HTTP. Era la raíz del 500 en login en producción.

### 3.2 reportes-service `@LoadBalanced` fix (`AggregatorClient.java`)
El cliente construía `RestClient` planos con defaults a `localhost`. En prod nunca había `routes.*`, así
que llamaba a `localhost:8082` → connection refused → 500 en el dashboard. Solución: inyectar el builder
`@LoadBalanced` (`@Qualifier("lbBuilder")`) y usar nombres de servicio Eureka (`http://vehiculos-service`).

### 3.3 DataInitializers (seed de demo)
Idempotentes (omiten si ya hay datos). Generan un dataset creíble para la demo:

| Servicio | Seed |
|---|---|
| personal | roles + **7 usuarios** across 5 roles (43 empleados totales) |
| vehiculos | 5 tipos + **75 vehículos** |
| cargas | **30 cargas** |
| rutas | 5 puntos de entrega + **40 despachos** |

### 3.4 Otros
- Dockerfiles multi-stage (Maven) en eureka + personal (y resto de servicios).
- `GlobalExceptionHandler` en cargas/vehiculos/rutas → formato de error consistente.
- `SecurityConfig`: rol **MOV** con acceso GET `/api/employees` (para que el form "Nueva Carga" cargue
  los movilizadores).

---

## 4. Deploy en Render

### 4.1 `render.yaml` (Blueprint)
1 PostgreSQL (`malvinas-db`, free, Oregon) + **8 web services** + 1 static site. Puntos clave:
- `SERVER_PORT: 10000` en todos (puerto que Render expone).
- `JWT_SECRET` con `sync: false` → se setea **a mano** en el dashboard, mismo valor en gateway/auth/personal.
- Credenciales de BD inyectadas con `fromDatabase` (host/db/user/password) — nunca hardcodeadas.
- `EUREKA_URI: https://malvinas-eureka.onrender.com` **sin** `/eureka` (el sufijo lo agrega el yml).
- `autoDeploy: false` → deploys manuales por servicio.
- Frontend static con regla `rewrite /* → /index.html`.

### 4.2 Servicios y URLs públicas

| Servicio | Nombre Render | URL |
|---|---|---|
| Eureka | `malvinas-eureka` | https://malvinas-eureka.onrender.com |
| API Gateway | `malvinas-api-gateway` | https://malvinas-api-gateway.onrender.com |
| personal | `malvinas-personal` | https://malvinas-personal.onrender.com |
| auth | `malvinas-auth` | https://malvinas-auth.onrender.com |
| vehiculos | `malvinas-vehiculos` | https://malvinas-vehiculos.onrender.com |
| cargas | `malvinas-cargas` | https://malvinas-cargas.onrender.com |
| rutas | `malvinas-rutas` | https://malvinas-rutas.onrender.com |
| reportes | `malvinas-reportes` | https://malvinas-reportes.onrender.com |
| frontend | `malvinas-frontend` | https://malvinas-frontend.onrender.com |

> El nombre `malvinas-api-gateway` es obligatorio: `environment.prod.ts` tiene esa URL hardcodeada.

### 4.3 Cold start (free tier)
Los servicios free "duermen" tras inactividad → primer request ~30s. Mitigación: **UptimeRobot** con ping
HTTP a `/actuator/health` cada 5 min por servicio. Antes de una demo en vivo, **calentar** los 8 servicios
5 min antes pegando cada `/actuator/health`.

---

## 5. Qué tanto podemos exponer — y por qué

Base para documentar el avance y decidir qué mostrar públicamente / en la exposición.

### ✅ Seguro de exponer
| Recurso | Por qué es seguro |
|---|---|
| URL del frontend y del gateway | Son el producto; no revelan secretos |
| Swagger UI por servicio (`/swagger-ui.html`) | Solo en perfil **dev**; documenta contratos, no datos sensibles |
| Dashboard de Eureka | Muestra servicios registrados, no datos de negocio |
| Repo de GitHub | El código no contiene secretos (ver abajo) |
| Credenciales **seed** (DNI + pass demo) | Datos **sintéticos**: sin clientes ni info real |
| Datos del sistema (vehículos, cargas, despachos) | Generados por DataInitializer, ficticios |

### 🔒 Protegido / NO expuesto
| Recurso | Cómo se protege |
|---|---|
| `JWT_SECRET` | `sync: false` en `render.yaml` → solo en el dashboard de Render, **nunca commiteado** |
| Archivos `.env` | Gitignored (`.env`, `.env.local`, `.env.*.local`) |
| Credenciales PostgreSQL | Inyectadas por Render vía `fromDatabase`, no en código ni en el repo |
| Swagger en producción | Deshabilitado por SpringDoc en perfil `prod` |
| BD PostgreSQL | `ipAllowList: []` → accesible solo desde servicios internos de Render |

### ⚠️ Riesgo controlado (consciente)
- El **seed usa contraseñas conocidas** (`admin123`, `sup12345`, …). Aceptable para una demo educativa
  con datos ficticios; **rotar/eliminar antes de cualquier uso real**.
- `ddl-auto: update` en prod (cómodo para iterar). Endurecer a Flyway + `validate` en el Paso 3.

---

## 6. Estado del proyecto

| Paso | Foco | Estado |
|---|---|---|
| **1** | Hardening + render.yaml + guía + slice login/personal | ✅ Completo |
| **2** | Completar vehículos/cargas/rutas/reportes + tests | ✅ Completo |
| **3** | Flyway + seguridad + extras + BD productiva | ✅ Completo |

## 7. Lo nuevo en V3 (rama `dev`)

### 7.1 Flujo TO-BE completo (rutas-service)
- `POST /api/dispatches/{id}/accept` (solo DRV): el conductor acepta su despacho asignado → genera Orden de Carga `OC-yyyyMMdd-NNNN`, marca `ON_ROUTE`, actualiza vehículo a EN_RUTA. Implementa RR-05/RR-06.
- `GET /api/dispatches/pending?driverId=` para que el DRV consulte sus despachos pendientes.
- `PersonalServiceClient` valida conductor activo (RR-03) en `create()` — con fallback tolerante (circuit breaker `personal-service` ya configurado).
- SUP/ADM mantienen `POST /{id}/departure` como override manual.
- SEC: acceso de lectura a despachos incluído explícitamente en `SecurityConfig`.

### 7.2 Notificaciones in-app + Notification API
- `core/services/notification.service.ts`: lista de notificaciones en Signals (max 50), método `notify()` que dispara toast + `Notification` nativa del browser (si pestaña oculta y permiso concedido). Permiso solicitado tras login exitoso.
- Detección por polling (sin backend nuevo): componente de rutas detecta cambios en cada refresh de 30s y emite notificaciones por rol.
- **DRV**: notificación inmediata cuando aparece un despacho SCHEDULED asignado; recordatorio cada 5 min (RR-05) mientras siga pendiente (`setInterval` en el servicio). Se cancela al aceptar.
- **SUP/ADM**: notifica cuando el DRV acepta (vehículo EN_RUTA) o se crean nuevos despachos.
- Campana con badge en el topbar (layout), panel desplegable con lista de notificaciones, marcar como leídas.
- Banner prominente para DRV en `/rutas` cuando tiene un despacho pendiente de aceptar, con botón "Aceptar salida" (pulsable en móvil, ≥44px touch target).
- Vista SEC: badge "OC vigente" junto al código de Orden de Carga.

### 7.3 Polish frontend móvil
- Tablas (`p-table`) → layout de tarjetas apiladas en `≤640px` via CSS puro (`data-label` + `display:block` en `tr`, `flex` en `td`). Sin cambios de componente.
- Columnas no esenciales ocultadas en móvil (`.hide-mobile`): Salida Prog., Salida Real en rutas; Planta, Inicio en cargas.
- Dialogs full-screen en móvil (`width: 100%; min-height: 100dvh`).
- Touch targets ≥44px (`min-height: 44px` en celdas de acción).
- Banner DRV con animación `pulse-border` para atención inmediata.

### 7.4 Tests (Paso 2 cerrado)
- H2 en scope test + `application-test.yml` (H2 MODE=PostgreSQL, esquema en INIT, ddl-auto: create-drop, flyway off, eureka off) en los 4 servicios JPA.
- `@Profile("!test")` en los 4 `DataInitializer` para evitar seed en tests.
- 1 test de flujo crítico por servicio (MockMvc + headers `X-Employee-*`): personal (GET employees), vehiculos (crear tipo + vehículo), cargas (crear + completar, `@MockitoBean` para vehiculos-client), rutas (crear + accept DRV, clients mockeados).
- `JwtServiceTest` (auth): unit test sin Spring, genera y valida claims.
- `JwtAuthFilterTest` (gateway): MockHttpServletRequest, token válido → headers, sin token → 401, /api/auth exento.
- `RateLimitFilterTest` (gateway): bajo límite pasa, sobre límite → 429, /actuator exento.
- `mvn clean test` verde sin PostgreSQL/Eureka.

### 7.5 Flyway (Paso 3)
- `flyway-core` + `flyway-database-postgresql` en los 4 poms JPA.
- `V1__init.sql` en `src/main/resources/db/migration/` por servicio (DDL completo del schema propio).
- `application-prod.yml`: `spring.flyway.enabled: true`, `schemas/default-schema`, `baseline-on-migrate: true` (BD Render existente → V1 se baselinea, no se re-ejecuta), `sql.init.mode: never`.
- `ddl-auto: ${JPA_DDL_AUTO:validate}` en prod — configurable con env var si se necesita `update` temporalmente.
- Dev: ddl-auto sigue en `update`, flyway deshabilitado — flujo de desarrollo sin fricción.

### 7.6 Rate limiting en gateway (Paso 3)
- `RateLimitFilter` (servlet, `@Order(0)`) con ventana fija por IP: `ConcurrentHashMap`, 120 req/min configurable (`ratelimit.requests-per-minute`), responde 429 JSON, exento `/actuator/**`. IP real via `X-Forwarded-For` (Render proxy). Evicción de entradas viejas en ~1% de requests.
- Sin dependencia nueva. `// ponytail: in-memory single-instance`.

### Pendiente manual (no en código)
- **Rotar `JWT_SECRET`** en dashboard de Render (obligatorio antes de usar en producción real).
- **Merge `dev` → `main`** + tag de versión tras smoke test final.
- **Verificar Flyway baseline** en Render: primer deploy con la nueva config ejecuta baseline en BD existente.
