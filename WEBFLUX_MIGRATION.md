# Plan de Migración: Spring MVC (Servlet) → Spring WebFlux (Reactivo)

> **Estado actual:** Todos los servicios usan `spring-boot-starter-web` (Servlet/Tomcat),
> `spring-data-jpa` (JDBC bloqueante) y `RestClient` para llamadas inter-servicio.
> El gateway usa `spring-cloud-starter-gateway-server-webmvc` (MVC-based).

---

## ¿Por qué migrar?

| Motivación | Detalle |
|------------|---------|
| **Concurrencia sin threads** | WebFlux usa Netty con event-loop; soporta miles de conexiones con pocos hilos |
| **Back-pressure** | El productor no satura al consumidor (Flux/Mono con Project Reactor) |
| **Menor memoria en Render** | Sin pool de threads bloqueantes → más headroom en free tier |
| **Integración moderna** | SSE (Server-Sent Events) para dashboard en tiempo real sin polling |
| **Gateway unificado** | `spring-cloud-starter-gateway` (WebFlux-native) es más potente que la versión MVC |

---

## Impacto por capa

```
Servlet Stack (actual)          Reactive Stack (objetivo)
─────────────────────────────   ─────────────────────────────
spring-boot-starter-web      →  spring-boot-starter-webflux
spring-data-jpa + Hibernate  →  spring-data-r2dbc + r2dbc-postgresql
RestClient / RestTemplate    →  WebClient
Tomcat                       →  Netty
@Transactional (JDBC)        →  @Transactional (R2DBC)
List<T> / T                  →  Flux<T> / Mono<T>
gateway-server-webmvc        →  spring-cloud-starter-gateway
```

---

## Fases de migración

### FASE 0 — Preparación (sin tocar código de producción)

1. **Crear rama `feature/reactive-migration`** en cada repositorio.
2. **Añadir tests de contrato** (Spring Cloud Contract o simple `@SpringBootTest` con `TestRestTemplate`)
   para cada endpoint existente — estos tests validan que el comportamiento no cambia tras la migración.
3. **Verificar compatibilidad de dependencias**:
   - Spring Boot 4.x → soporta R2DBC y WebFlux nativamente.
   - `mapstruct` 1.6+ → compatible con tipos reactivos (el mapper sigue siendo síncrono; la conversión ocurre dentro del stream).
   - `resilience4j-reactor` → reemplaza `resilience4j-spring-boot3` para operadores reactivos.

---

### FASE 1 — Gateway (migración independiente, sin BD)

El gateway no tiene base de datos, solo enruta. Es el cambio más seguro para empezar.

**`malvinas-api-gateway/pom.xml`**
```xml
<!-- Eliminar -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-server-webmvc</artifactId>
</dependency>

<!-- Agregar -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>  <!-- WebFlux-native -->
</dependency>
```

**`application.yml`** — cambiar prefijo de rutas:
```yaml
# Antes (MVC)
spring.cloud.gateway.mvc.routes:

# Después (WebFlux)
spring.cloud.gateway.routes:
```

**`JwtAuthFilter.java`** — el filtro pasa de `HandlerInterceptor` a `GatewayFilter` reactivo:
```java
// Antes (MVC)
public class JwtAuthFilter implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) { ... }
}

// Después (WebFlux)
@Component
public class JwtAuthFilter implements GatewayFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);
        // validar token...
        return chain.filter(exchange);
    }
}
```

**`SecurityConfig.java`** — reemplazar `SecurityFilterChain` por `SecurityWebFilterChain`:
```java
// Antes
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) { ... }

// Después
@Bean
public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(ex -> ex.anyExchange().permitAll())
        .build();
}
```

**Entregable:** Gateway desplegado en Netty, rutas funcionando igual que antes.

---

### FASE 2 — auth-service (sin BD propia, solo WebClient)

auth-service no tiene JPA, solo llama a personal-service. Migración mínima.

**`pom.xml`**
```xml
<!-- Eliminar -->
<dependency>spring-boot-starter-web</dependency>

<!-- Agregar -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**`RestClientConfig.java`** → `WebClientConfig.java`
```java
@Configuration
public class WebClientConfig {

    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @LoadBalanced
    @Qualifier("lbWebClient")
    public WebClient.Builder lbWebClientBuilder() {
        return WebClient.builder();
    }
}
```

**`PersonalServiceClient.java`** — `RestClient` → `WebClient`
```java
@Component
public class PersonalServiceClient {

    private final WebClient webClient;

    public PersonalServiceClient(@Qualifier("lbWebClient") WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://personal-service").build();
    }

    public Mono<EmployeeAuthInfo> findByDni(String dni) {
        return webClient.get()
            .uri("/api/employees/by-dni/{dni}", dni)
            .retrieve()
            .bodyToMono(EmployeeAuthInfo.class)
            .transform(CircuitBreakerOperator.of(circuitBreaker));
    }
}
```

**`AuthController.java`** — retornar tipos reactivos:
```java
// Antes
@PostMapping("/login")
public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest req) {
    return ResponseEntity.ok(service.login(req));
}

// Después
@PostMapping("/login")
public Mono<ResponseEntity<TokenResponse>> login(@RequestBody LoginRequest req) {
    return service.login(req)
        .map(ResponseEntity::ok);
}
```

**`AuthServiceImpl.java`** — cadena reactiva:
```java
public Mono<TokenResponse> login(LoginRequest req) {
    return personalClient.findByDni(req.dni())
        .switchIfEmpty(Mono.error(new UnauthorizedException("Credenciales inválidas")))
        .flatMap(emp -> {
            if (!passwordEncoder.matches(req.password(), emp.passwordHash()))
                return Mono.error(new UnauthorizedException("Credenciales inválidas"));
            return Mono.just(buildTokenResponse(emp));
        });
}
```

---

### FASE 3 — reportes-service (sin BD, solo WebClient)

Similar a auth. Solo reemplazar `RestClient` con `WebClient` y cambiar `AggregatorClient`.

**`AggregatorClient.java`**
```java
@Component
public class AggregatorClient {

    private final WebClient vehiculosClient;
    private final WebClient cargasClient;
    private final WebClient rutasClient;

    public AggregatorClient(@Qualifier("lbWebClient") WebClient.Builder builder) {
        this.vehiculosClient = builder.clone().baseUrl("http://vehiculos-service").build();
        this.cargasClient    = builder.clone().baseUrl("http://cargas-service").build();
        this.rutasClient     = builder.clone().baseUrl("http://rutas-service").build();
    }

    public Flux<Map<String, Object>> getVehicles() {
        return vehiculosClient.get().uri("/api/vehicles")
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    // Combinar múltiples llamadas en paralelo (no posible con RestClient síncrono):
    public Mono<DashboardResponse> getDashboard() {
        return Mono.zip(
            getVehicles().collectList(),
            getActiveLoads().collectList(),
            getActiveDispatches().collectList()
        ).map(tuple -> buildDashboard(tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }
}
```

> **Ventaja clave:** `Mono.zip()` lanza las 3 llamadas HTTP **en paralelo**, reduciendo latencia del
> dashboard de `t1+t2+t3` a `max(t1,t2,t3)`. Con `RestClient` síncrono son secuenciales.

---

### FASE 4 — vehiculos-service (JPA → R2DBC)

Esta es la migración más profunda. JPA/Hibernate no soporta programación reactiva; se reemplaza con R2DBC.

**`pom.xml`**
```xml
<!-- Eliminar -->
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>postgresql (JDBC driver)</dependency>

<!-- Agregar -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**`application.yml`** — cambiar datasource por r2dbc:
```yaml
# Antes
spring:
  datasource:
    url: jdbc:postgresql://host:5432/db
    username: user
    password: pass
  jpa:
    hibernate:
      ddl-auto: create

# Después
spring:
  r2dbc:
    url: r2dbc:postgresql://host:5432/db
    username: user
    password: pass
  sql:
    init:
      mode: always          # ejecuta schema.sql al inicio
      schema-locations: classpath:schema.sql
```

**Entidades — cambios en anotaciones:**
```java
// Antes (JPA)
@Entity
@Table(name = "vehicles", schema = "vehicles")
public class Vehicle extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;           // ← relaciones lazy NO existen en R2DBC
}

// Después (R2DBC)
@Table(name = "vehicles")                      // sin schema en la anotación (se configura en URL)
public class Vehicle {
    @Id
    private Long id;

    @Column("vehicle_type_id")
    private Long vehicleTypeId;                // ← solo la FK, no la relación

    @Transient
    private VehicleType vehicleType;           // ← se carga manualmente con join reactivo
}
```

> **Limitación de R2DBC:** No soporta `@OneToMany`, `@ManyToOne`, ni `@Embeddable` de JPA.
> Las relaciones deben cargarse explícitamente con joins en el servicio o con `DatabaseClient`.

**Repositorios — `JpaRepository` → `ReactiveCrudRepository`:**
```java
// Antes
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStatusAndIsActiveTrue(VehicleStatus status);
}

// Después
public interface VehicleRepository extends ReactiveCrudRepository<Vehicle, Long> {
    Flux<Vehicle> findByStatusAndIsActiveTrue(String status);   // R2DBC usa String para el campo
}
```

**`AuditableEntity`** — eliminar `@MappedSuperclass` (no aplica en R2DBC):
```java
// R2DBC no tiene herencia de entidades con @MappedSuperclass.
// Los campos de auditoría se repiten en cada entidad, o se usan
// ReactiveAuditorAware + @CreatedDate/@LastModifiedDate de Spring Data.
```

**Servicios — todo retorna `Mono<T>` o `Flux<T>`:**
```java
// Antes
public VehicleResponse create(VehicleRequest req) {
    Vehicle v = new Vehicle();
    // setters...
    return vehicleMapper.toResponse(vehicleRepo.save(v));
}

// Después
public Mono<VehicleResponse> create(VehicleRequest req) {
    return typeRepo.findById(req.vehicleTypeId())
        .switchIfEmpty(Mono.error(new ResourceNotFoundException("VehicleType", req.vehicleTypeId())))
        .flatMap(type -> {
            Vehicle v = new Vehicle();
            // setters...
            return vehicleRepo.save(v);
        })
        .map(vehicleMapper::toResponse);
}
```

**Converters JPA → R2DBC:**
```java
// Antes (JPA AttributeConverter)
@Converter(autoApply = true)
public class VehicleStatusConverter implements AttributeConverter<VehicleStatus, String> { ... }

// Después (R2DBC usa EnumWritingConverter / EnumReadingConverter de Spring Data)
@WritingConverter
public class VehicleStatusWritingConverter implements Converter<VehicleStatus, String> {
    public String convert(VehicleStatus source) { return source.getCode(); }
}

@ReadingConverter
public class VehicleStatusReadingConverter implements Converter<String, VehicleStatus> {
    public VehicleStatus convert(String source) {
        return DisplayableEnum.fromCode(VehicleStatus.class, source);
    }
}

// Registrar en R2dbcCustomConversions:
@Bean
public R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory cf) {
    return R2dbcCustomConversions.of(
        PostgresDialect.INSTANCE,
        new VehicleStatusWritingConverter(),
        new VehicleStatusReadingConverter()
    );
}
```

---

### FASE 5 — cargas-service, personal-service, rutas-service

Mismos pasos que vehiculos-service (FASE 4). Aplicar en este orden para reducir riesgo:

1. `cargas-service` — menor complejidad relacional
2. `personal-service` — tiene `@OneToMany` (Employee → Attendances), requiere carga manual
3. `rutas-service` — mayor complejidad (`Dispatch` → `DispatchPoints` → `DeliveryPoint`)

**Patrón para relaciones `@OneToMany` en R2DBC:**
```java
// En DispatchService, cargar puntos manualmente:
public Mono<DispatchResponse> findById(Long id) {
    return dispatchRepo.findById(id)
        .flatMap(dispatch ->
            dispatchPointRepo.findByDispatchId(dispatch.getId())
                .collectList()
                .flatMap(points ->
                    Flux.fromIterable(points)
                        .flatMap(pt -> deliveryPointRepo.findById(pt.getDeliveryPointId())
                            .map(dp -> { pt.setDeliveryPoint(dp); return pt; }))
                        .collectList()
                )
                .map(points -> { dispatch.setDispatchPoints(points); return dispatch; })
        )
        .map(mapper::toDto);
}
```

---

### FASE 6 — Eureka Server (sin cambios de código)

Eureka Server no tiene lógica de negocio ni BD. El cambio es solo en cómo los clientes se registran:

- Los clientes reactivos siguen usando `spring-cloud-starter-netflix-eureka-client`.
- No requiere cambios en `EurekaServerApplication.java`.
- Verificar que `eureka.client.webclient.enabled=true` esté en cada microservicio cliente (habilita registro vía WebClient en lugar de RestTemplate).

```yaml
# En cada microservicio (application.yml)
eureka:
  client:
    webclient:
      enabled: true
```

---

### FASE 7 — Frontend (sin cambios)

Angular consume el API Gateway vía HTTP normal. El frontend no necesita cambios.
La única mejora opcional: reemplazar polling del dashboard por **SSE (Server-Sent Events)**:

```typescript
// Antes (polling cada 10s)
interval(10000).pipe(
  switchMap(() => this.dashboardService.getKpis())
).subscribe(...);

// Después (SSE desde reportes-service)
const eventSource = new EventSource(`${apiUrl}/api/kpis/stream`);
eventSource.onmessage = (event) => {
  this.kpis.set(JSON.parse(event.data));
};
```

```java
// En ReportesController (WebFlux)
@GetMapping(value = "/kpis/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<KpiResponse> streamKpis() {
    return Flux.interval(Duration.ofSeconds(5))
        .flatMap(tick -> aggregatorClient.buildKpis());
}
```

---

## Tabla de compatibilidad de dependencias

| Dependencia actual | Equivalente reactivo | Notas |
|--------------------|----------------------|-------|
| `spring-boot-starter-web` | `spring-boot-starter-webflux` | Netty en lugar de Tomcat |
| `spring-data-jpa` | `spring-data-r2dbc` | Sin lazy loading, sin JPQL |
| `postgresql` (JDBC) | `r2dbc-postgresql` | Mismo schema PostgreSQL |
| `spring-cloud-starter-gateway-server-webmvc` | `spring-cloud-starter-gateway` | Ya es WebFlux-native |
| `RestClient` | `WebClient` | API similar, tipos reactivos |
| `resilience4j-spring-boot3` | `resilience4j-reactor` | Operadores `CircuitBreakerOperator.of()` |
| `spring-boot-starter-security` | `spring-boot-starter-security` | Sin cambio, pero `SecurityWebFilterChain` |
| `mapstruct` | `mapstruct` (sin cambio) | El mapper es síncrono; se usa dentro de `.map()` |
| `@Transactional` (JDBC) | `@Transactional` (R2DBC) | `TransactionalOperator` para chains complejos |
| `JPA AttributeConverter` | `R2dbcCustomConversions` | `@WritingConverter` / `@ReadingConverter` |

---

## Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| R2DBC no soporta relaciones JPA | Alto | Cargar relaciones manualmente con `flatMap` + repositorio secundario |
| `@MappedSuperclass` no aplica en R2DBC | Medio | Repetir campos de auditoría, o usar `@CreatedDate`/`@LastModifiedDate` de Spring Data Reactive Auditing |
| Debugging más complejo (stack traces reactivos) | Medio | Activar `Hooks.onOperatorDebug()` en desarrollo |
| Flyway no soporta R2DBC directamente | Bajo | Usar `flyway-core` con conexión JDBC separada solo para migraciones, o `liquibase-core` con R2DBC support |
| Curva de aprendizaje de Project Reactor | Medio | Migrar servicio a servicio, no todo junto |
| `spring-security` reactivo (WebFlux) es diferente | Medio | Revisar `SecurityWebFilterChain` vs `SecurityFilterChain` |

---

## Orden de migración recomendado

```
1. malvinas-api-gateway          ← sin BD, bajo riesgo
2. malvinas-auth-service         ← sin BD, solo WebClient
3. malvinas-reportes-service     ← sin BD, mayor beneficio en paralelismo
4. malvinas-vehiculos-service    ← BD simple (pocas relaciones)
5. malvinas-cargas-service       ← BD simple
6. malvinas-personal-service     ← BD con OneToMany
7. malvinas-rutas-service        ← BD más compleja (mayor riesgo)
8. malvinas-eureka-server        ← solo config, último
```

---

## Verificación por fase

- **Fase 1 (Gateway):** Las mismas rutas responden igual; el JWT se valida igual.
- **Fase 2 (Auth):** Login retorna token; refresh y logout funcionan.
- **Fase 3 (Reportes):** Dashboard carga más rápido (llamadas paralelas).
- **Fases 4-7 (Servicios con BD):** Los tests de contrato creados en Fase 0 pasan sin modificación.
- **Fase 7 (Rutas):** Flujo completo end-to-end: carga → despacho → cierre.

---

## Nota final

La migración **no es obligatoria** para que el sistema funcione correctamente.
El stack actual (MVC + JPA) es sólido, bien entendido y suficiente para 75 vehículos.

Considerar la migración si:
- Se necesita SSE para el dashboard en tiempo real.
- El número de vehículos/usuarios escala significativamente.
- El costo de memoria en Render free tier se convierte en limitante.
- El equipo ya tiene experiencia con Project Reactor.
