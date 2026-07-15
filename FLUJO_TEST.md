# Malvinas SOA — Guía de Smoke Test

Flujo completo TO-BE: desde MOV carga un vehículo hasta DRV cierra la ruta.
Cubre todas las funcionalidades implementadas en V3.

---

## Prerequisitos

### 1. Levantar servicios (orden obligatorio)

```bash
# Terminal 1
cd malvinas-eureka-server && mvn spring-boot:run

# Terminal 2 (esperar a que Eureka esté en http://localhost:8761)
cd malvinas-api-gateway && mvn spring-boot:run

# Terminales 3-8 (cualquier orden)
cd malvinas-personal-service  && mvn spring-boot:run
cd malvinas-auth-service      && mvn spring-boot:run
cd malvinas-vehiculos-service && mvn spring-boot:run
cd malvinas-cargas-service    && mvn spring-boot:run
cd malvinas-rutas-service     && mvn spring-boot:run
cd malvinas-reportes-service  && mvn spring-boot:run

# Terminal 9
cd malvinas-frontend && npm start
```

### 2. Verificar servicios registrados

Abrir http://localhost:8761 → deben aparecer los 6 servicios de negocio.

### 3. Credenciales demo

| Rol | DNI | Contraseña |
|-----|-----|------------|
| Administrador | 00000001 | admin123 |
| Supervisor | 00000011 | sup12345 |
| Movilizador | 00000021 | mov12345 |
| Conductor | 00000031 | drv12345 |
| Seguridad | 00000071 | sec12345 |

---

## Flujo TO-BE completo

### PASO 1 — Login como MOV (Movilizador)

1. Abrir http://localhost:4200
2. Ingresar DNI `00000021` / `mov12345`
3. Verificar: redirige a `/cargas`
4. ✅ Comprobar: menú solo muestra **Cargas**

---

### PASO 2 — MOV: Crear carga

1. En Cargas → clic **Nueva Carga**
2. Seleccionar cualquier vehículo en estado "Disponible" (ej. primer resultado)
3. El campo "Movilizador" se autocompleta con tu usuario
4. Agregar al menos un producto (descripción + peso)
5. Clic **Guardar**
6. ✅ Verificar: la carga aparece en la tabla con estado **En Carga**
7. ✅ Verificar: el vehículo cambió a estado **En Carga** en vehiculos-service

---

### PASO 3 — MOV: Completar carga

1. En la tabla de Cargas, encontrar la carga recién creada
2. Clic **Completar carga** → confirmar en el dialog
3. ✅ Estado cambia a **Completada**
4. ✅ El vehículo pasa a **Cargado**
5. Cerrar sesión

---

### PASO 4 — Login como SUP (Supervisor)

1. Ingresar DNI `00000011` / `sup12345`
2. ✅ Menú muestra: Dashboard, Vehículos, Cargas, Rutas, Reportes

---

### PASO 5 — SUP: Crear despacho

1. Ir a **Rutas** → clic **Nuevo Despacho**
2. Seleccionar el vehículo que el MOV cargó (estado "Cargado")
3. Seleccionar conductor: ej. Pedro Ramirez Soto (DNI 00000031)
4. Definir hora de salida programada
5. Agregar al menos 1 punto de entrega
6. Clic **Crear Despacho**
7. ✅ Despacho aparece con estado **Programado**

---

### PASO 6 — SUP: Verificar notificación

1. ✅ Campana (🔔) en el topbar debe mostrar badge rojo
2. Clic en la campana → panel de notificaciones
3. ✅ Aparece notificación: "Despacho asignado"
4. Cerrar sesión

---

### PASO 7 — Login como DRV (Conductor — Pedro Ramirez Soto)

1. Ingresar DNI `00000031` / `drv12345`
2. ✅ Menú solo muestra **Rutas**
3. ✅ **Banner naranja pulsante** en la parte superior de Rutas:
   > "Tienes un despacho pendiente de aceptar"
4. ✅ Campana con badge de notificación no leída

**Probar recordatorio (opcional):**
- Dejar la pestaña abierta 5 minutos sin aceptar
- ✅ Aparece nueva notificación recordatorio

---

### PASO 8 — DRV: Aceptar despacho

1. En el banner naranja o en la tabla → clic **Aceptar salida**
2. ✅ Banner desaparece
3. ✅ Despacho pasa a estado **En Ruta**
4. ✅ Columna OC muestra código generado: `OC-YYYYMMDD-XXXX`
5. ✅ El vehículo cambió a estado **En Ruta**
6. ✅ Notificación desaparece del recordatorio (clearInterval)
7. Dejar sesión abierta

---

### PASO 9 — Abrir segunda pestaña como SEC (Seguridad)

1. Abrir http://localhost:4200 en **pestaña nueva** (incognito)
2. Ingresar DNI `00000071` / `sec12345`
3. ✅ Solo ve **Rutas** (modo lectura)
4. ✅ En la tabla, el despacho aceptado muestra badge **OC vigente** junto al código
5. ✅ No hay botones de acción en la columna de acciones
6. ✅ El código OC es legible en fuente monospace

---

### PASO 10 — DRV: Cerrar ruta

1. Volver a la sesión del DRV
2. En la tabla → clic **Cerrar Ruta** (o botón completo)
3. ✅ Estado pasa a **Completado**
4. ✅ El vehículo vuelve a **Disponible**
5. ✅ SUP/ADM reciben notificación "Ruta cerrada"

---

### PASO 11 — Verificar reportes (SUP/ADM)

1. Login como SUP
2. Ir a **Dashboard**
3. ✅ KPIs actualizados: despachos completados, cargas del día
4. Ir a **Reportes**
5. ✅ Tablas reflejan el flujo recién ejecutado

---

## Tests de notificaciones

### Notification API nativa (browser)

1. Login en cualquier cuenta
2. ✅ El browser debe pedir permiso de notificaciones tras el login
3. Minimizar el navegador o cambiar a otra pestaña
4. Realizar una acción que genere notificación (ej: SUP crea despacho para DRV)
5. ✅ Aparece notificación del sistema operativo

### Gestión del panel de notificaciones

1. Acumular varias notificaciones
2. Clic en campana → panel abre
3. ✅ Contador badge coincide con no leídas
4. Clic en una notificación → navega a la ruta correspondiente + marca como leída
5. Clic **Marcar todas leídas** → badge desaparece

---

## Tests mobile / responsive

Abrir DevTools → Toggle Device Toolbar → iPhone SE (375px)

| Elemento | Comportamiento esperado |
|---|---|
| Tabla de Cargas | Filas → tarjetas apiladas con `data-label:` visible |
| Tabla de Rutas | Ídem; columnas "Salida Prog." y "Salida Real" ocultas |
| Dialogs (Nueva Carga, Nuevo Despacho) | Full-screen (100% width, 100dvh) |
| Botones de acción en tabla | Altura ≥ 44px (touch target) |
| Banner DRV | Ocupa ancho completo, pulsa en naranja |
| Menú hamburguesa | Sidebar se abre/cierra con overlay |

---

## Tests de seguridad / rate limiting

### Rate limiting (gateway)

```bash
# Más de 120 requests en 1 minuto desde la misma IP → 429
for i in $(seq 1 125); do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/employees; done
# Las últimas respuestas deben ser 429
```

### Actuator exento del rate limit

```bash
for i in $(seq 1 200); do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/actuator/health; done
# Todas deben ser 200
```

### JWT requerido

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/employees
# Debe retornar 401
```

### Auth path exento de JWT

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"dni":"00000001","password":"admin123"}'
# Debe retornar 200 con tokens (no 401)
```

---

## Tests de backend (sin infra)

```bash
# Desde la raíz del proyecto
mvn clean test

# O por servicio individual
mvn test -pl malvinas-personal-service
mvn test -pl malvinas-vehiculos-service
mvn test -pl malvinas-cargas-service
mvn test -pl malvinas-rutas-service
mvn test -pl malvinas-auth-service
mvn test -pl malvinas-api-gateway
```

**Resultado esperado:** todos los módulos en BUILD SUCCESS, sin PostgreSQL ni Eureka corriendo.

---

## Remember-me

1. Login con cualquier cuenta → activar checkbox "Recordar mis datos"
2. Cerrar el navegador completamente
3. Abrir de nuevo http://localhost:4200
4. ✅ DNI y contraseña pre-cargados (cifrados en localStorage)
5. Login sin remember-me → ✅ sesión en sessionStorage, no persiste tras cierre

---

## Flyway (solo en deploy/BD limpia)

Al hacer deploy en Render con BD vacía:
1. Flyway ejecuta `V1__init.sql` → crea esquemas y tablas
2. `ddl-auto: validate` verifica que el DDL coincide con las entidades

Con BD Render existente (ya tiene tablas):
1. Flyway ve que no existe `flyway_schema_history` → hace baseline en V1
2. V1 se marca como ejecutado sin correr → Hibernate solo valida

```bash
# Verificar baseline en logs del servicio en Render:
# "Successfully baselined schema ... to version 1"
```

---

## Checklist final antes de merge

- [ ] `mvn clean test` en verde (raíz, sin infra)
- [ ] `npm run build:prod` en verde
- [ ] Flujo TO-BE completo ejecutado (pasos 1-11)
- [ ] Notificaciones browser activadas y funcionando
- [ ] Vista móvil revisada en DevTools (375px)
- [ ] Rate limiting verificado (429 en límite, actuator exento)
- [ ] Remember-me: guarda y restaura credenciales
- [ ] Commit por fases en rama `dev`
- [ ] Deploy en Render + smoke test en prod
- [ ] Merge `dev` → `main` + tag de versión
- [ ] Rotar `JWT_SECRET` en dashboard de Render
