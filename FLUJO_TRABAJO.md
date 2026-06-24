# Flujo de Trabajo — Grupo Malvina
## Sistema de Gestión de Carga y Despacho Vehicular (SOA)

---

## 1. Contexto del Negocio

Grupo Malvina es una empresa de transporte y distribución de bebidas (Cristal, Inca Kola, San Luis,
Gloria, etc.) que opera una flota de **75 vehículos** (vans, camiones medianos, camiones grandes,
furgones y minivans) desde el centro de distribución **Babel - Huachipa** hacia clientes retail
(Tambo, Listo, OXXO) en Lima Metropolitana.

---

## 2. Proceso AS-IS (Estado Actual — Sin Sistema)

El proceso actual es **100% manual**, coordinado por WhatsApp y llamadas telefónicas entre roles.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PROCESO MANUAL DE DESPACHO (AS-IS)                           │
└─────────────────────────────────────────────────────────────────────────────────┘

  [TURNO MAÑANA 5:30am]

  MOVILIZADOR          SUPERVISOR              CONDUCTOR            SEGURIDAD
  ─────────────        ──────────────          ─────────────        ─────────────
       │                     │                      │                    │
  1. Verifica            2. Recibe info          3. Espera            4. Anota
     vehículos              por WhatsApp            asignación           manualmente
     disponibles            de MOV                  por WhatsApp         la salida
       │                     │                      │                    │
  3. Asigna carga        4. Llama a DRV          5. Carga el          5. Firma
     manualmente             para confirmar         vehículo             papel de
     en planilla             disponibilidad         manualmente          portería
       │                     │                      │                    │
  5. Registra en        6. Genera OC en         6. Sale sin          6. Sin registro
     Excel o papel          Word/Excel              trazabilidad         digital
       │                     │                      │                    │
  ←─ ~15 min de demora en cada paso ──────────────────────────────────────────────→
```

### Problemas del AS-IS
| Problema | Impacto |
|----------|---------|
| ~15 min de lag por despacho | Retrasos acumulados en toda la flota |
| 20% de error en registros manuales | Inconsistencias en cuentas y KPIs |
| 47% de flota sin trazabilidad real | No se sabe en tiempo real qué vehículos están disponibles |
| Coordinación 100% por WhatsApp | Sin historial auditable, mensajes se pierden |
| Sin control de estado de vehículos | Mantenimientos no planificados causan demoras |
| KPIs calculados manualmente | Reportes tardíos, decisiones basadas en datos obsoletos |

---

## 3. Proceso TO-BE (Sistema SOA Implementado)

El sistema digitaliza y orquesta el proceso completo en **6 fases** con trazabilidad completa.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PROCESO DIGITAL DE DESPACHO (TO-BE)                          │
└─────────────────────────────────────────────────────────────────────────────────┘

FASE 1           FASE 2           FASE 3           FASE 4           FASE 5 & 6
─────────        ─────────        ─────────        ─────────        ────────────

MOV inicia       SUP verifica     SUP programa     DRV registra     DRV cierra
proceso de       la carga y       el despacho      la salida        la ruta al
carga en         asigna           en el sistema    → Sistema        retornar a
el sistema       conductor        → OC generada    genera OC        base
                                  automática       (OC-YYYYMMDD-N)

Estado VEH:      Estado VEH:      Estado VEH:      Estado VEH:      Estado VEH:
AVAILABLE        LOADING ─→       LOADED ─→        ON_ROUTE ─→      AVAILABLE
                 LOADED           LOADED (hasta     ON_ROUTE         (vuelve)
                                  despacho)

Estado CARGA:    Estado CARGA:    Estado DESPACHO: Estado DESPACHO: Estado DESPACHO:
PENDIENTE ─→     EN_PROCESO ─→    SCHEDULED ─→     ON_ROUTE ─→      COMPLETED
EN_PROCESO       COMPLETADA       ON_ROUTE         ON_ROUTE
```

### Ventajas del TO-BE
| Mejora | Valor |
|--------|-------|
| Registro digital en tiempo real | Visibilidad inmediata del estado de flota |
| Orden de Carga (OC) generada automáticamente | Formato: `OC-YYYYMMDD-NNN` |
| Trazabilidad completa | Audit trail con `createdAt`, `modifiedAt`, `createdBy` |
| Estados de vehículo en tiempo real | Dashboard actualizado al instante |
| KPIs automáticos | `reportes-service` agrega datos de todos los servicios |
| Control de acceso por rol | Cada usuario solo ve y puede hacer lo que le corresponde |

---

## 4. Mapa de Roles

### ADM — Administrador
- **Quién:** Gerente de operaciones, jefatura
- **Acceso:** Total — todos los módulos
- **Puede hacer:**
  - Gestión completa de empleados (crear, editar roles, activar/desactivar)
  - Gestión completa de flota (crear, editar, cambiar estado de vehículos)
  - Ver y gestionar cargas y despachos de cualquier usuario
  - Acceso a todos los reportes y KPIs
  - Configurar tipos de vehículos

### SUP — Supervisor
- **Quién:** Supervisor de operaciones en turno
- **Acceso:** Vehiculos, Cargas, Rutas, Reportes
- **Puede hacer:**
  - Ver y editar estado de vehículos
  - Verificar cargas iniciadas por MOV → marcar como completadas
  - Programar despachos → asignar conductor (DRV) y vehículo
  - Registrar salida de despachos (cuando el camión sale de la planta)
  - Ver reportes operacionales

### MOV — Movilizador
- **Quién:** Operario de planta/patio
- **Acceso:** Cargas
- **Puede hacer:**
  - Iniciar un proceso de carga (selecciona vehículo disponible + su propio ID)
  - Ver cargas en proceso
  - NO puede programar despachos ni editar vehículos

### DRV — Conductor
- **Quién:** Chofer de camión/furgón
- **Acceso:** Rutas
- **Puede hacer:**
  - Ver despachos programados asignados a su ID
  - Registrar salida de su despacho (genera la OC automáticamente)
  - Cerrar la ruta al retornar a base
  - NO puede crear ni cancelar despachos

### SEC — Seguridad
- **Quién:** Personal de portería/garita
- **Acceso:** Rutas (solo lectura)
- **Puede hacer:**
  - Ver listado de despachos activos y en ruta
  - Verificar órdenes de carga (OC) al momento de la salida del vehículo
  - **NO puede** crear, editar ni cambiar estado de ningún registro

---

## 5. Flujo Principal Detallado por Servicio

```
personal-service ──── Valida empleados y roles
        │
auth-service ──────── Emite JWT con { sub: employeeId, role: "ADM"|"SUP"|... }
        │
api-gateway ───────── Valida JWT → inyecta X-Employee-Id, X-Employee-Role, X-Employee-Name
        │
  ┌─────┴──────────────────────────────────────────┐
  │                                                  │
vehiculos-service                               cargas-service
(flota, estado, tipos)                          (proceso de carga)
  │                                                  │
  └──────────┬───────────────────────────────────────┘
             │
         rutas-service
         (despachos, puntos de entrega, OC)
             │
         reportes-service
         (agrega KPIs de los 3 servicios anteriores)
```

### Estados de Vehículo
```
AVAILABLE (01) ──→ LOADING (02) ──→ LOADED (03) ──→ ON_ROUTE (04) ──→ AVAILABLE (01)
     ↑                                                                        │
     └──────────────────────────── MAINTENANCE (05) ←────────────────────────┘
```

### Estados de Carga
```
PENDIENTE (01) ──→ EN_PROCESO (02) ──→ COMPLETADA (03)
                         └──────────→ CANCELADA (04)
```

### Estados de Despacho
```
SCHEDULED (01) ──→ ON_ROUTE (02) ──→ COMPLETED (03)
     └───────────────────────────→ CANCELLED (04)
```

---

## 6. Modelo de Datos Cruzados

Los servicios están en esquemas separados dentro de la misma instancia PostgreSQL:

| Esquema | Tablas principales | Servicio |
|---------|-------------------|----------|
| `staff` | `roles`, `employees`, `attendances` | personal-service |
| `vehicles` | `vehicle_types`, `vehicles`, `status_history` | vehiculos-service |
| `loads` | `loads`, `load_details` | cargas-service |
| `routes` | `delivery_points`, `dispatches`, `dispatch_points` | rutas-service |

Las referencias cruzadas (ej. `vehiclePlate` en cargas, `driverId` en rutas) son **cadenas/IDs**
sin FK entre esquemas — consistencia mantenida por lógica de negocio en la capa de servicio.

---

## 7. Credenciales de Demo (Seed Data)

> Las contraseñas están almacenadas con BCrypt. El sistema compara usando `passwordEncoder.matches()`.

| DNI | Email | Contraseña | Rol |
|-----|-------|-----------|-----|
| 00000001 | admin@malvinas.pe | `admin123` | ADM |
| 00000002 | director@malvinas.pe | `admin123` | ADM |
| 00000011 | cquispe@malvinas.pe | `sup12345` | SUP |
| 00000012 | rflores@malvinas.pe | `sup12345` | SUP |
| 00000013 | jmendez@malvinas.pe | `sup12345` | SUP |
| 00000021 | ltorres@malvinas.pe | `mov12345` | MOV |
| 00000031 | pedro1@malvinas.pe | `drv12345` | DRV |
| 00000071 | msanchez@malvinas.pe | `sec12345` | SEC |

**Login:** El sistema acepta DNI (no email) + contraseña en el formulario de acceso.

---

## 8. Procedimiento de Base de Datos Limpia

Si el seed no corrió o los datos están corruptos, ejecutar en psql:

```sql
-- Conectar a malvinas_db
\c malvinas_db

-- Limpiar schemas (borra TODOS los datos y tablas)
DROP SCHEMA IF EXISTS staff    CASCADE;
DROP SCHEMA IF EXISTS vehicles CASCADE;
DROP SCHEMA IF EXISTS loads    CASCADE;
DROP SCHEMA IF EXISTS routes   CASCADE;

-- Recrear schemas vacíos
CREATE SCHEMA staff;
CREATE SCHEMA vehicles;
CREATE SCHEMA loads;
CREATE SCHEMA routes;
```

Luego reiniciar los servicios en orden:
1. `malvinas-eureka-server` (puerto 8761)
2. `malvinas-api-gateway` (puerto 8080)
3. `malvinas-personal-service` (8081) — siembra roles + 43 empleados
4. `malvinas-auth-service` (8086)
5. `malvinas-vehiculos-service` (8082) — siembra 5 tipos + 75 vehículos
6. `malvinas-cargas-service` (8083) — siembra 30 cargas
7. `malvinas-rutas-service` (8084) — siembra 5 puntos de entrega + 40 despachos
8. `malvinas-reportes-service` (8085) — sin BD propia, agrega en tiempo real
9. `malvinas-frontend` — `npm start` desde `malvinas-frontend/`

Los DataInitializers son idempotentes: si la BD ya tiene datos, el seed se omite sin error.
