# Guía de Deploy en Render — Malvinas SOA

> Esta guía enseña cómo desplegar el sistema completo en Render paso a paso.
> Tiempo estimado: 45–90 minutos la primera vez.

---

## Conceptos clave antes de empezar

### ¿Qué es Render?
Render es una plataforma de hosting en la nube (como Heroku) que puede correr contenedores Docker, servicios web, y sitios estáticos. Para este proyecto usamos:
- **Web Service (Docker)**: cada microservicio Spring Boot corre en su propio contenedor.
- **Static Site**: el frontend Angular se compila y se sirve como archivos estáticos (sin servidor).
- **PostgreSQL**: base de datos administrada.

### Free tier — limitaciones
- Los servicios **duermen después de 15 minutos sin tráfico** (cold start de ~30s cuando se despiertan).
- **Solución:** UptimeRobot hace un ping HTTP cada 5 minutos → los servicios nunca duermen.
- 1 PostgreSQL gratuita por cuenta (nuestro caso: una instancia, 4 schemas).
- Los servicios Docker en free tier pueden tener RAM limitada; los flags JVM del Dockerfile (`-XX:MaxRAMPercentage=75.0`) previenen OOM.

### Routing del frontend — Hash Location Strategy
El frontend Angular usa **hash routing** (`/#/dashboard`, `/#/rutas`, etc.). Esto significa que el servidor solo recibe `/` en todos los casos — el fragmento `#/...` es manejado por Angular en el cliente. No hay riesgo de 404 al recargar la página o compartir una URL directa, independientemente del servidor de hosting que se use.

> Nota técnica: el `render.yaml` también incluye una regla `/* → /index.html` como capa extra de seguridad, pero con hash routing es redundante.

### Cómo funciona el Blueprint (`render.yaml`)
El archivo `render.yaml` en la raíz del repositorio define toda la infraestructura como código.
Render lo lee y crea todos los servicios automáticamente. Solo necesitas:
1. Conectar el repositorio de GitHub a Render.
2. Ir a **Dashboard → New → Blueprint** y seleccionar el repo.
3. Render crea todos los servicios y la BD definidos en `render.yaml`.

---

## Prerequisitos

1. Cuenta en [render.com](https://render.com) (gratuita).
2. Repositorio en GitHub con el código del proyecto (`dev` branch pusheado).
3. `openssl` instalado (para generar el JWT secret).

### Generar el JWT Secret (hacerlo ANTES de empezar)

```bash
openssl rand -base64 32
```

Guarda el resultado, por ejemplo:
```
K8mX2pQjR9sLvN4wA7dY1cF5bH3eG6iJ0kMnOpTuVwXyZ=
```

Este valor se usará en tres servicios: `personal-service`, `auth-service`, `api-gateway`. **Deben tener el mismo valor exacto.**

---

## Paso 1: Subir el código a GitHub

Si no lo has hecho todavía:

```bash
cd C:\projects\malvinas_SOA
git checkout dev
git add .
git commit -m "Paso 1: Dockerfiles + render.yaml + deploy guide"
git remote add origin https://github.com/TU-USUARIO/malvinas-soa.git
git push -u origin dev
```

> **Reemplaza** `TU-USUARIO` con tu nombre de usuario de GitHub.

---

## Paso 2: Crear el Blueprint en Render

1. Entra a [render.com](https://render.com) → **Dashboard**.
2. Haz clic en **New +** → **Blueprint**.
3. Conecta tu cuenta de GitHub si no lo has hecho.
4. Selecciona el repositorio `malvinas-soa`.
5. Rama: `dev`.
6. Render detectará el `render.yaml` y mostrará los 9 recursos a crear.
7. Haz clic en **Apply** para crear todos los recursos.

> Render creará todos los recursos pero **no los desplegará todavía** — necesitas configurar las variables `sync: false` primero.

---

## Paso 3: Configurar JWT_SECRET en los 3 servicios

El `JWT_SECRET` está marcado como `sync: false` en `render.yaml` porque es un secreto que no debe estar en el código. Debes configurarlo manualmente:

1. En el Dashboard → selecciona **malvinas-personal** → **Environment**.
2. Encuentra la variable `JWT_SECRET` → haz clic en el ícono de editar.
3. Pega el JWT secret que generaste antes → **Save Changes**.
4. Repite lo mismo para **malvinas-auth** y **malvinas-api-gateway**.

> **CRÍTICO:** los tres servicios deben tener el **mismo** valor de `JWT_SECRET`. Si difieren, el gateway rechazará tokens válidos con 401.

---

## Paso 4: Crear los schemas de PostgreSQL

Render crea la base de datos `malvinas_db`, pero los schemas (`staff`, `vehicles`, `loads`, `routes`) debes crearlos manualmente:

1. Dashboard → **malvinas-db** → copia la **External Connection String** (tiene usuario, contraseña, host y puerto).
2. Conéctate con psql (o cualquier cliente como DBeaver):
   ```bash
   psql "postgresql://malvinas_user:PASSWORD@HOST:5432/malvinas_db"
   ```
3. Ejecuta:
   ```sql
   CREATE SCHEMA IF NOT EXISTS staff;
   CREATE SCHEMA IF NOT EXISTS vehicles;
   CREATE SCHEMA IF NOT EXISTS loads;
   CREATE SCHEMA IF NOT EXISTS routes;
   ```
4. Verifica:
   ```sql
   \dn
   ```
   Deberías ver los 4 schemas listados.

> **Nota:** `ddl-auto: update` crea las tablas automáticamente cuando el servicio arranca por primera vez. No necesitas crear tablas manualmente.

---

## Paso 5: Desplegar los servicios (en orden)

Los servicios tienen dependencias entre sí. Debes desplegarlos en este orden:

### 5.1 malvinas-eureka (primero siempre)

1. Dashboard → **malvinas-eureka** → **Manual Deploy** → **Deploy latest commit**.
2. Espera hasta que el status sea **Live** y el health check pase.
3. Verifica: abre `https://malvinas-eureka.onrender.com/actuator/health`
   Deberías ver: `{"status":"UP"}`
4. (Opcional) Dashboard de Eureka: `https://malvinas-eureka.onrender.com`
   Muestra los servicios registrados — al principio estará vacío.

> **¿Por qué primero?** Todos los demás servicios se registran en Eureka al arrancar. Si Eureka no está disponible, los otros servicios fallan en el arranque.

### 5.2 malvinas-personal

1. Dashboard → **malvinas-personal** → **Manual Deploy**.
2. El build tarda ~3–5 minutos (descarga de dependencias Maven).
3. Al arrancar, el `DataInitializer` crea automáticamente:
   - Roles: ADM, SUP, MOV, DRV, SEC
   - Empleado admin: `DNI: 00000001 / password: admin123`
4. Verifica: `https://malvinas-personal.onrender.com/actuator/health`

> **¿Cómo sé que el seed funcionó?** En los logs del servicio (Dashboard → Logs) verás:
> `Inicializando roles del sistema... Roles creados: 5`
> `Admin creado: admin@malvinas.pe / admin123`

### 5.3 malvinas-auth

1. Deploy **malvinas-auth** (espera que personal esté Live).
2. auth-service llama a personal-service via Eureka para validar credenciales.
3. Verifica: `https://malvinas-auth.onrender.com/actuator/health`

### 5.4 malvinas-api-gateway

1. Deploy **malvinas-api-gateway**.
2. Es el punto de entrada único para el frontend.
3. Verifica: `https://malvinas-api-gateway.onrender.com/actuator/health`

### 5.5 malvinas-frontend (Static Site)

1. Dashboard → **malvinas-frontend** → **Manual Deploy**.
2. El build Angular tarda ~2–3 minutos.
3. Verifica: abre la URL del static site (algo como `https://malvinas-frontend.onrender.com`).
4. Deberías ver la pantalla de login.

---

## Paso 6: Verificar el flujo completo (login + personal)

> **Antes de empezar — calentar los servicios (evitar cold start):**
> Si los servicios estuvieron inactivos, el primer request puede tardar ~30s.
> Abre estas URLs en el navegador y espera `{"status":"UP"}` en cada una antes de probar el login:
> - `https://malvinas-eureka.onrender.com/actuator/health`
> - `https://malvinas-personal.onrender.com/actuator/health`
> - `https://malvinas-auth.onrender.com/actuator/health`
> - `https://malvinas-api-gateway.onrender.com/actuator/health`

1. Abre el frontend en el navegador.
2. Ingresa: DNI `00000001` / contraseña `admin123`.
3. Deberías aterrizar en el **Dashboard** y ver los KPIs y el gráfico de flota.
4. Navega a **Personal** → deberías ver los empleados del seed.
5. Prueba crear un nuevo empleado.

> **Auto-refresh activo:** Las páginas de Rutas, Cargas y Dashboard se actualizan automáticamente cada 30 segundos. No es necesario recargar la página para ver cambios hechos por otros usuarios.

**¡El Paso 1 del deploy está completo!**

---

## Paso 6.5: Verificar acceso por rol

Después del deploy, prueba cada rol para confirmar que los guards y el routing funcionan correctamente:

| Rol | DNI | Contraseña | Aterriza en | Puede acceder a | No puede acceder a |
|---|---|---|---|---|---|
| ADM | 00000001 | admin123 | /dashboard | Todo | — |
| SUP | 00000011 | sup12345 | /dashboard | /cargas, /rutas, /reportes | /personal |
| MOV | 00000021 | mov12345 | /cargas | /cargas | /dashboard, /personal, /vehiculos, /rutas, /reportes |
| DRV | 00000031 | drv12345 | /rutas | /rutas (solo sus despachos) | Todo lo demás |
| SEC | 00000071 | sec12345 | /rutas | /rutas (solo lectura) | Todo lo demás |

**Qué verificar por rol:**
- **ADM:** Dashboard muestra KPIs + gráfico de flota con colores → menú lateral muestra todas las opciones → puede crear/editar empleados en /personal.
- **SUP:** Dashboard visible → intentar ir a `/personal` redirige a `/dashboard` → en /cargas puede completar cargas iniciadas por MOV.
- **MOV:** Al logear va directo a /cargas → botón "Nueva Carga" visible → puede iniciar carga seleccionando vehículo disponible + movilizador → intentar ir a `/dashboard` redirige a `/cargas`.
- **DRV:** Al logear va a /rutas → tabla muestra **solo** despachos asignados a ese conductor → no aparece botón "Nuevo Despacho" → puede registrar salida (icono avión) y cerrar ruta (icono casa).
- **SEC:** Al logear va a /rutas → ve todos los despachos activos → **ningún botón de acción** es visible → solo lectura.

---

## Paso 7: Configurar UptimeRobot (evitar cold starts)

Sin UptimeRobot, los servicios dormirán después de 15 minutos de inactividad. La próxima petición tarda ~30s en despertarlos, lo que hace que el login falle con timeout.

1. Crea cuenta en [uptimerobot.com](https://uptimerobot.com) (gratuita).
2. Para cada servicio, crea un **HTTP(s) Monitor**:

**Paso 1 (slice login/personal):**

| Monitor | URL | Intervalo |
|---|---|---|
| malvinas-eureka | `https://malvinas-eureka.onrender.com/actuator/health` | 5 minutos |
| malvinas-personal | `https://malvinas-personal.onrender.com/actuator/health` | 5 minutos |
| malvinas-auth | `https://malvinas-auth.onrender.com/actuator/health` | 5 minutos |
| malvinas-api-gateway | `https://malvinas-api-gateway.onrender.com/actuator/health` | 5 minutos |

**Paso 2 (sistema completo — agregar estos):**

| Monitor | URL | Intervalo |
|---|---|---|
| malvinas-vehiculos | `https://malvinas-vehiculos.onrender.com/actuator/health` | 5 minutos |
| malvinas-cargas | `https://malvinas-cargas.onrender.com/actuator/health` | 5 minutos |
| malvinas-rutas | `https://malvinas-rutas.onrender.com/actuator/health` | 5 minutos |
| malvinas-reportes | `https://malvinas-reportes.onrender.com/actuator/health` | 5 minutos |

3. Verifica que los monitores estén en **UP** (verde). Con los 8 monitores activos, ningún servicio dormirá durante el horario operativo.

---

## Paso 8: Deploy completo (Paso 2 — todos los servicios)

Cuando el código de los servicios restantes esté listo:

```
Deploy order:
malvinas-vehiculos → malvinas-cargas → malvinas-rutas → malvinas-reportes
```

Cada uno sigue el mismo flujo: Manual Deploy → espera Live → verifica health.

---

## Troubleshooting

### El servicio queda en "Failed" después del deploy

**Causa más común 1:** El health check falla porque el servicio tarda en arrancar.
- **Solución:** Los servicios Spring Boot con muchas dependencias (Eureka client, JPA, etc.) pueden tardar 60–90 segundos. Render da 5 minutos por defecto. Si sigue fallando, revisa los logs.

**Causa más común 2:** La base de datos no tiene los schemas creados.
- **Solución:** Conectarte a la BD y ejecutar los `CREATE SCHEMA` del Paso 4.

**Causa más común 3:** `JWT_SECRET` no configurado.
- **Solución:** Revisar la variable de entorno en Dashboard → Environment.

### El login falla con 401 o 403

**Causa:** `JWT_SECRET` diferente entre los tres servicios (personal, auth, gateway).
- **Solución:** En el Dashboard de cada uno, verificar que `JWT_SECRET` sea exactamente el mismo valor.

### El login falla con 503 o timeout

**Causa 1:** El servicio está durmiendo (cold start).
- **Solución:** Espera 30 segundos y reintenta. Después configura UptimeRobot.

**Causa 2:** eureka-server está caído y personal-service no puede registrarse.
- **Solución:** Verifica que `malvinas-eureka` esté Live. Re-deploy personal/auth/gateway después de que eureka esté estable.

### "Service not found" en Eureka

**Causa:** El servicio no se registró todavía (puede tardar hasta 30s después de arrancar).
- **Verificación:** Abre `https://malvinas-eureka.onrender.com` y verifica que el servicio aparezca en la lista.
- **Solución si no aparece:** Revisar que `EUREKA_URI` esté configurado correctamente (sin `/eureka` al final).

### Eureka clients no se conectan

Verifica la variable `EUREKA_URI` en cada servicio:
- **Correcto:** `https://malvinas-eureka.onrender.com`
- **Incorrecto:** `https://malvinas-eureka.onrender.com/eureka` (el `/eureka` lo agrega `application-prod.yml`)

### Los schemas de la BD no se crearon

El error en los logs sería algo como: `ERROR: schema "staff" does not exist`
- **Solución:** Conectarse a la BD y ejecutar los `CREATE SCHEMA` (ver Paso 4).

---

## Variables de entorno — referencia completa

| Variable | Quién la usa | Fuente |
|---|---|---|
| `ENVIRONMENT` | Todos los servicios | `prod` (fijo) |
| `SERVER_PORT` | Todos los servicios | `10000` (fijo — Render escucha en este puerto) |
| `EUREKA_URI` | Todos menos eureka-server | URL del servicio `malvinas-eureka` (sin `/eureka`) |
| `EUREKA_HOSTNAME` | Solo eureka-server | Hostname del servicio eureka en Render |
| `JWT_SECRET` | personal, auth, gateway | Generado con `openssl rand -base64 32` — MISMO en los 3 |
| `POSTGRESQL_HOST` | personal, vehiculos, cargas, rutas | Del servicio PostgreSQL de Render |
| `POSTGRESQL_DB` | personal, vehiculos, cargas, rutas | `malvinas_db` |
| `POSTGRESQL_USER` | personal, vehiculos, cargas, rutas | `malvinas_user` |
| `POSTGRESQL_PASS` | personal, vehiculos, cargas, rutas | Password de la BD en Render |

> `render.yaml` configura todas las variables automáticamente excepto `JWT_SECRET` (`sync: false`), que debes poner manualmente por seguridad.

---

## URLs del sistema desplegado

| Servicio | URL |
|---|---|
| Frontend | `https://malvinas-frontend.onrender.com` |
| API Gateway | `https://malvinas-api-gateway.onrender.com` |
| Eureka Dashboard | `https://malvinas-eureka.onrender.com` |
| Health checks | `https://<servicio>.onrender.com/actuator/health` |

> Los endpoints Swagger (`/swagger-ui.html`) están deshabilitados en prod por configuración de SpringDoc.

---

## Credenciales de demo (seed completo)

El `DataInitializer` de cada servicio siembra los siguientes usuarios. Úsalos para probar todos los roles tras el deploy:

| DNI | Contraseña | Rol | Aterriza en | Descripción |
|---|---|---|---|---|
| 00000001 | admin123 | ADM | /dashboard | Gerente — acceso total |
| 00000002 | admin123 | ADM | /dashboard | Director — acceso total |
| 00000011 | sup12345 | SUP | /dashboard | Supervisor de turno |
| 00000012 | sup12345 | SUP | /dashboard | Supervisor de turno |
| 00000021 | mov12345 | MOV | /cargas | Movilizador de patio |
| 00000031 | drv12345 | DRV | /rutas | Conductor (ve solo sus despachos) |
| 00000071 | sec12345 | SEC | /rutas | Seguridad — solo lectura |

> **Nota MOV:** El formulario "Nueva Carga" del movilizador carga la lista de empleados desde `personal-service`. El endpoint `GET /api/employees` ya tiene permiso para el rol MOV — no requiere configuración adicional.

> **Seguridad:** Cambia las contraseñas antes de cualquier demo pública con clientes reales.
