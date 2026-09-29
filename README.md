# LicitaWatch

**Plataforma privada de gestión de licitaciones** — Proyecto de Título (Capstone) 2026.

Los **Licitadores** publican y administran licitaciones privadas, las **Pymes** las buscan, filtran y postulan, y el **Administrador** gestiona la plataforma. La operación se financia con planes **Estándar** (gratis) y **Premium** ($24.990 vía **Webpay Plus**). Incluye **LicitAsist**, un asistente con IA (Groq · `openai/gpt-oss-120b`) que responde con los datos reales de la plataforma.

> Todo corre **on-premise en Windows**, sin Docker: PostgreSQL local + 6 procesos Java + frontend Vite.

---

## 1. Descripción

| Perfil | Puede |
|---|---|
| **Licitador** | Crear, editar, publicar, cerrar, reabrir, cancelar y eliminar sus licitaciones; definir requisitos, condiciones y fecha de cierre; revisar postulantes; seleccionar al ganador (adjudicar). |
| **Pyme** | Buscar y filtrar licitaciones (categoría, región, monto, cierre); ver detalle y requisitos; postular; editar o retirar su postulación; seguir su estado. |
| **Administrador** | CRUD de usuarios, activar/desactivar, cambiar roles; moderar (suspender/reactivar/eliminar) licitaciones; consultar postulaciones, ventas, pagos, suscripciones, notificaciones y estadísticas; usar LicitAsist. |

Funcionalidades transversales: registro separado por perfil con confirmación de correo, login JWT, planes con límites mensuales, pago Webpay Plus con voucher (HTML, PDF y correo), historial de pagos, notificaciones por email (SMTP) y LicitAsist.

**Fuera de alcance (PPT):** scraping de licitaciones públicas, app móvil nativa, motor propio de ML e integración con ERP.

## 2. Arquitectura

```
                 ┌──────────────────────────────┐
  Navegador ───▶ │ Frontend React + TS  :5173   │
                 └──────────────┬───────────────┘
                                │ HTTP (JWT)
                 ┌──────────────▼───────────────┐
                 │ API Gateway          :8080   │  JWT · RBAC · CORS · Rate limit · SQL injection
                 └──┬──────┬──────┬──────┬───┬──┘
                    │      │      │      │   │
   ┌────────────────▼┐ ┌───▼────────┐ ┌──▼─────────┐ ┌▼──────────────┐ ┌▼──────────────┐
   │ API Usuarios    │ │ API        │ │ API Ventas │ │ API           │ │ API Asistente │
   │ :8081           │ │ Licitaciones│ │ (MS-Ventas)│ │ Notificaciones│ │ LicitAsist    │
   │                 │ │ :8082      │ │ :8083      │ │ :8084         │ │ :8085 (sin BD)│
   └───────┬─────────┘ └────┬───────┘ └────┬───────┘ └──────┬────────┘ └──────┬────────┘
           │                │              │                │                 │ Groq API
     licitawatch_      licitawatch_   licitawatch_    licitawatch_
     usuarios          licitaciones   ventas          notificaciones   (PostgreSQL, una BD por servicio)
```

- Cada microservicio tiene **su propia base PostgreSQL**. No existen FK ni consultas entre bases: los IDs externos (`licitador_id`, `pyme_id`, `usuario_id`) se validan por **REST**.
- Comunicación servicio a servicio por REST en `/internal/**` con el header `X-Internal-Key` (nunca expuesto por el Gateway). Sin bus de mensajería: Licitaciones y MS-Ventas llaman directo a Notificaciones.
- Cada microservicio **vuelve a validar** JWT, roles y ownership. El Gateway no es la única capa de seguridad.

**Modelo de datos (tal cual la PPT):**

| Servicio | Tablas oficiales (PPT) | Tablas de soporte 1:1 |
|---|---|---|
| Usuarios | `rol`, `usuario`, `licitador`, `pyme`, `administrador` | `cuenta_usuario` (hash BCrypt, confirmación), `token_verificacion` |
| Licitaciones | `licitacion`, `postulacion` | `licitacion_detalle`, `postulacion_detalle`, `busqueda_registro` |
| Ventas | `plan_suscripcion`, `suscripcion`, `venta`, `pago` | `plan_detalle`, `suscripcion_vigencia`, `pago_webpay` |
| Notificaciones | `notificacion` | `notificacion_envio` |

Los `enum` de la PPT son tipos **ENUM nativos de PostgreSQL**. Los datos técnicos que la PPT no modela van en tablas de soporte separadas, para no alterar las tablas oficiales.

## 3. Tecnologías y versiones

### Backend (Java)

| Tecnología | Versión | Uso |
|---|---|---|
| Java (JDK) | **21** (LTS) | Lenguaje de los 6 servicios |
| Apache Maven | **3.9+** | Build y dependencias (POM agregador en la raíz) |
| Spring Boot | **3.3.13** | Web, Data JPA, Security, Validation, Actuator, Mail |
| Spring Cloud Gateway | **2023.0.6** | API Gateway (`api-gateway`) |
| Flyway | 10.x (gestionado por Spring Boot) | Migraciones de cada base |
| PostgreSQL JDBC | gestionado por Spring Boot | Driver de base de datos |
| JJWT | **0.12.6** | Emisión y validación de JWT |
| Bucket4j + Caffeine | **8.10.1** | Rate limiting en el Gateway |
| springdoc-openapi | **2.6.0** | Swagger UI / OpenAPI |
| Transbank SDK Java | **6.2.1** | Webpay Plus |
| OpenPDF | **2.0.3** | Voucher en PDF |
| Thymeleaf + JavaMail | gestionado por Spring Boot | Plantillas y envío de correos |
| Lombok | gestionado por Spring Boot | Reducción de código repetitivo |

### Frontend

| Tecnología | Versión | Uso |
|---|---|---|
| Node.js | **20+** (probado con 24) | Entorno de ejecución |
| npm | **10+** | Gestor de paquetes |
| React / React DOM | **18.3** | UI |
| TypeScript | **5.6** | Tipado |
| Vite | **5.4** | Dev server y build |
| Tailwind CSS | **3.4** | Estilos |
| React Router DOM | 6.27 | Rutas |
| Zustand | 4.5 | Estado (sesión, toasts) |
| Axios | 1.7 | Cliente HTTP |
| Recharts | 2.13 | Gráficos del panel |
| lucide-react, react-markdown, remark-gfm | 0.453 / 9 / 4 | Íconos y render de respuestas de LicitAsist |

### Base de datos, pruebas e integraciones

| Tecnología | Versión | Uso |
|---|---|---|
| PostgreSQL | **14+** (probado con 18.6) | Una base por microservicio |
| JUnit 5 + MockMvc / WebTestClient | gestionado por Spring Boot | Pruebas de integración |
| H2 | gestionado por Spring Boot | Base en memoria para pruebas (modo PostgreSQL) |
| GreenMail | 2.0.1 | SMTP en memoria para pruebas |
| Postman + Newman | Newman vía `npx` | Pruebas de API end-to-end |
| Webpay Plus (Transbank) | ambiente SANDBOX | Pagos de prueba |
| Groq API | modelo `openai/gpt-oss-120b` | LicitAsist (IA) |
| SMTP (Gmail) | puerto 587 STARTTLS | Correos transaccionales |

## 4. Requisitos para ejecutar el proyecto

**Obligatorios** (sin esto el proyecto no arranca):

| Requisito | Versión mínima | Verificar con |
|---|---|---|
| Java JDK | 21 | `java -version` |
| Apache Maven | 3.9 | `mvn -v` |
| Node.js + npm | 20 / 10 | `node -v` · `npm -v` |
| PostgreSQL (servidor local en `localhost:5432`) | 14 | `psql --version` |
| Archivo `.env` en la raíz (copiado de `.env.example`) | — | ver sección 8 |

**Necesarios solo para algunas funciones** (el resto del sistema funciona sin ellos):

| Requisito | Para qué | Sin él… |
|---|---|---|
| Conexión a internet | Webpay, Groq y SMTP | No hay pagos, IA ni correos |
| API key de Groq (`GROQ_API_KEY`) | LicitAsist | El asistente responde error |
| Cuenta SMTP (p. ej. contraseña de aplicación de Gmail) | Confirmación de registro y avisos | Las notificaciones quedan `FALLIDO`; hay que confirmar cuentas a mano |
| Postman o Newman | Ejecutar la colección de pruebas | — |

Sistema operativo: Windows 10/11 (los scripts `scripts\*.ps1` son PowerShell). En Linux/macOS funciona levantando los servicios manualmente (sección 10). **No se usa Docker.**

## 5. Instalación

```bash
git clone https://github.com/PabloMaximilianoBG/Licitawatch.git LicitaWatch
cd LicitaWatch
copy .env.example .env        # y complétalo (sección 8)
cd frontend && npm install && cd ..
mvn -DskipTests package       # compila los 6 servicios Java
```

## 6. PostgreSQL

1. Instala PostgreSQL desde https://www.postgresql.org/download/windows/ (incluye `psql` en `C:\Program Files\PostgreSQL\<versión>\bin`).
2. Durante la instalación define la contraseña del usuario `postgres`. En este equipo: usuario **`postgres`**, contraseña configurada en `.env` (`POSTGRES_PASSWORD`).
3. Verifica que el servicio `postgresql-x64-<versión>` esté iniciado y escuchando en `localhost:5432`.

## 7. Creación de bases

Una base por microservicio (API Asistente no tiene BD):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\crear-bases.ps1
```

o manualmente:

```bash
psql -U postgres -h localhost -f db/crear-bases.sql
```

Crea `licitawatch_usuarios`, `licitawatch_licitaciones`, `licitawatch_ventas` y `licitawatch_notificaciones`. **Las tablas las crea Flyway** al arrancar cada servicio.

## 8. Variables de entorno

Todas en el archivo **`.env` de la raíz**, que está en `.gitignore` y nunca se sube. Plantilla: `.env.example`. Los servicios Java lo leen con `spring.config.import`, y el frontend solo lee las variables `VITE_*`.

| Grupo | Variables |
|---|---|
| PostgreSQL | `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_USUARIOS_NAME`, `DB_LICITACIONES_NAME`, `DB_VENTAS_NAME`, `DB_NOTIFICACIONES_NAME` |
| JWT | `JWT_SECRET` (≥ 32 caracteres, el mismo en todos los servicios), `JWT_EXPIRATION` (minutos) |
| Interna | `INTERNAL_API_KEY` |
| Webpay | `WEBPAY_COMMERCE_CODE`, `WEBPAY_API_KEY`, `WEBPAY_ENVIRONMENT` (`SANDBOX`/`INTEGRATION`/`PRODUCTION`), `WEBPAY_RETURN_URL` |
| Groq | `GROQ_API_KEY`, `GROQ_MODEL=openai/gpt-oss-120b`, `GROQ_API_BASE_URL` |
| SMTP | `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`, `SMTP_STARTTLS` |
| Admin | `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME`, `ADMIN_AREA` |
| Rate limit | `RATE_LIMIT_GENERAL_PER_MINUTE`, `RATE_LIMIT_AUTH_PER_MINUTE`, `RATE_LIMIT_ASISTENTE_PER_MINUTE` |
| URLs | `FRONTEND_URL`, `USUARIOS_URL`, `LICITACIONES_URL`, `VENTAS_URL`, `NOTIFICACIONES_URL`, `ASISTENTE_URL`, `VITE_API_BASE_URL` |

> **Tildes y ñ en `.env`:** Spring lee el archivo como ISO-8859-1. Usa escapes Unicode, por ejemplo `ADMIN_AREA=Administraci\u00f3n`.

Genera secretos aleatorios con `openssl rand -base64 48`, o en PowerShell: `[Convert]::ToBase64String((1..48|%{Get-Random -Max 256}))`.

## 9. Migraciones Flyway

Cada servicio tiene sus migraciones en `src/main/resources/db/migration` y Flyway las aplica automáticamente al arrancar:

- `api-usuarios`: `V1__create_rol` … `V5__create_administrador` (modelo PPT), `V6__create_cuenta_usuario`, `V7__create_token_verificacion`.
- `api-licitaciones`: `V1__create_licitacion`, `V2__create_postulacion`, `V3`–`V5` soporte, `V6__create_indices`.
- `api-ventas`: `V1__create_plan_suscripcion` … `V4__create_pago` (modelo PPT), `V5__create_plan_detalle` (precios y límites), `V6`, `V7__create_pago_webpay`.
- `api-notificaciones`: `V1__create_notificacion`, `V2__create_notificacion_envio`.

Para ver el historial: `select * from flyway_schema_history;` en cada base.

## 10. Ejecución de microservicios

**Opción recomendada (todo de una vez):**

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1          # backend + frontend
powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1 -Build   # recompila antes
powershell -ExecutionPolicy Bypass -File scripts\stop-all.ps1           # detiene todo
```

Levanta en orden Usuarios → Licitaciones → Ventas → Notificaciones → Asistente → Gateway → Frontend, espera a que cada uno responda `/actuator/health` y deja los logs en `.run\`.

**Opción manual** (una terminal por servicio, desde su carpeta):

```bash
cd api-usuarios && mvn spring-boot:run      # 8081
cd api-licitaciones && mvn spring-boot:run  # 8082
cd api-ventas && mvn spring-boot:run        # 8083
cd api-notificaciones && mvn spring-boot:run # 8084
cd api-asistente && mvn spring-boot:run     # 8085
```

> Si Java muestra `Unable to establish loopback connection`, la ruta de TEMP tiene espacios. Agrega `-Djdk.net.unixdomain.tmpdir=C:\licitawatch-tmp`. El script `start-all.ps1` ya lo hace.

Swagger / OpenAPI de cada servicio: `http://localhost:8081/swagger-ui.html` (también 8082, 8083, 8084 y 8085).

## 11. Ejecución Gateway

```bash
cd api-gateway && mvn spring-boot:run       # 8080
```

Es el **único punto de entrada** del frontend. Rutas: `/api/auth/**`, `/api/usuarios/**`, `/api/licitaciones/**`, `/api/postulaciones/**`, `/api/planes/**`, `/api/suscripciones/**`, `/api/ventas/**`, `/api/pagos/**`, `/api/notificaciones/**`, `/api/asistente/**` y `/api/admin/**`. Aplica, en este orden: bloqueo de `/internal` (404), filtro de SQL injection (400), rate limit con Bucket4j (429 con `Retry-After`), JWT (401), RBAC (403) y CORS (solo `FRONTEND_URL`).

## 12. Ejecución Frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
npm run build      # build de producción (dist/)
```

## 13. Configuración Webpay

Webpay Plus corre en **SANDBOX**, el ambiente de integración de Transbank, **sin cobros reales**. Usa las credenciales públicas de pruebas que ya vienen en `.env.example` (`597055555532` / API key de integración) y `WEBPAY_ENVIRONMENT=SANDBOX`.

Flujo: `/planes` → `/pago` → `POST /api/ventas` (venta, pago y suscripción PENDIENTE) → `POST /api/pagos/webpay/transacciones` (token y URL) → formulario de Transbank → Webpay vuelve a `WEBPAY_RETURN_URL` (Gateway) → **el backend hace el commit con Transbank** → valida AUTHORIZED, código de respuesta, monto y orden → activa Premium por 30 días → correo con voucher y PDF → `/pago/resultado`.

**Tarjetas de prueba:**

| Resultado | Tarjeta | CVV | Autenticación |
|---|---|---|---|
| Aprobada | VISA `4051 8856 0044 6623` | `123` | RUT `11.111.111-1`, clave `123` |
| Rechazada | Mastercard `5186 0595 5959 0568` | `123` | RUT `11.111.111-1`, clave `123` |

Para pasar a producción: código de comercio y API key reales de Transbank, `WEBPAY_ENVIRONMENT=PRODUCTION` y una `WEBPAY_RETURN_URL` pública con HTTPS.

## 14. Configuración Groq

1. Crea una API key en https://console.groq.com/keys.
2. Define `GROQ_API_KEY` en `.env` (modelo: `GROQ_MODEL=openai/gpt-oss-120b`).

LicitAsist verifica el acceso **en el backend**: el ADMINISTRADOR siempre tiene acceso, mientras que PYME y LICITADOR solo con Premium activo (si no, responde 403 `PREMIUM_REQUERIDO`). El contexto se arma consultando las APIs con el JWT del propio usuario, así que cada servicio aplica sus permisos. Solo se envían a Groq campos de una lista blanca: nunca contraseñas, JWT, secretos, tokens de pago ni emails innecesarios.

## 15. Configuración SMTP

Con Gmail: activa la verificación en dos pasos y crea una **contraseña de aplicación** (https://myaccount.google.com/apppasswords). Luego:

```
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=tu_correo@gmail.com
SMTP_PASSWORD=<contraseña de aplicación de 16 caracteres>
SMTP_FROM=tu_correo@gmail.com
SMTP_STARTTLS=true
```

Correos que se envían: confirmación de registro, licitación publicada, postulación recibida, postulación enviada, resultado de postulación, licitación adjudicada, pago confirmado (voucher y PDF), pago rechazado y avisos del administrador. Si SMTP falla, la notificación queda registrada como `FALLIDO` y el flujo de negocio continúa.

## 16. Credenciales iniciales de administrador

Al arrancar, **API Usuarios crea el administrador si no existe**, con los datos de `.env` y la contraseña guardada con BCrypt (nunca queda en el código):

```
ADMIN_EMAIL=admin@licitawatch.local
ADMIN_PASSWORD=AdminLicitawatch2026!
ADMIN_NAME=Administrador
ADMIN_AREA=Administraci\u00f3n
```

Cambia `ADMIN_PASSWORD` antes de cualquier uso fuera de desarrollo. Si el admin ya existe, no se modifica.

## 17. Ejecución de pruebas

```bash
mvn test                     # todos los servicios (desde la raíz)
cd api-usuarios && mvn test  # un servicio
cd frontend && npx tsc -b --noEmit && npm run build
```

| Servicio | Pruebas |
|---|---|
| api-usuarios | 8 (RUT, registro → correo → confirmación → login, validaciones y duplicados, CRUD admin, roles, 401/403, `/internal`) |
| api-licitaciones | 5 (flujo publicar → postular → seleccionar, ownership, estados, límites de plan, moderación) |
| api-ventas | 4 (Webpay aprobado/rechazado/monto alterado/sin completar, idempotencia, comprobante y PDF, seguridad, admin) |
| api-notificaciones | 4 (SMTP real contra GreenMail, voucher con PDF adjunto, escape XSS, SMTP caído) |
| api-asistente | 4 (acceso Premium/Admin, contexto sin datos sensibles, validaciones) |
| api-gateway | 7 (enrutamiento, 401, RBAC, SQLi, 429, `/internal`, CORS) |

Las pruebas usan H2 en modo PostgreSQL. Solo se simulan los bordes externos (otros microservicios, Transbank y Groq); la lógica, la persistencia y la seguridad son reales.

## 18. Importación de Postman

1. Postman → **Import** → `postman/LicitaWatch.postman_collection.json` y `postman/LicitaWatch.postman_environment.json`.
2. Selecciona el environment **LicitaWatch · Local**.
3. Opcional: ajusta `emailPrueba` (buzón que recibe los correos mediante alias `+tag`), `jwtSecret` (para el test de JWT expirado), `confirmToken` y `pagoCompletado`.
4. Ejecuta la colección **en orden** con el Collection Runner. Los JWT e IDs se guardan solos.

Por línea de comandos:

```bash
npx newman run postman/LicitaWatch.postman_collection.json -e postman/LicitaWatch.postman_environment.json --env-var "jwtSecret=<JWT_SECRET>"
```

157 requests en 11 carpetas, con pruebas positivas y negativas (400, 401, 403, 404, 409, 429 y 5xx). El test de 429 va al final porque bloquea el login durante 1 minuto. Para probar un pago aprobado: abre `postman/pagar-webpay.html?token=<webpayToken>`, paga con la tarjeta de prueba, define `pagoCompletado=si` y vuelve a ejecutar la carpeta 06. Si modificas la colección, regénerala con `python scripts/dev/build_postman.py`.

## 19. Ejecución del flujo completo

1. `scripts\start-all.ps1` y abre **http://localhost:5173**.
2. **Pyme:** Registrarme como Pyme → correo "Confirmar correo" → iniciar sesión → buscar y filtrar → ver detalle → postular → Planes → Contratar Premium → pagar en Webpay (tarjeta aprobada) → resultado con voucher y correo con PDF → Suscripción → Historial de pagos → Ver comprobante → LicitAsist.
3. **Licitador:** Registrarme como Licitador → confirmar correo → crear y publicar licitación (correo de publicación) → revisar postulantes → seleccionar (ambas partes reciben correo) → Premium → LicitAsist.
4. **Administrador:** login con las credenciales de `.env` → Estadísticas → Usuarios y roles → Licitaciones (moderar) → Postulaciones → Ventas y pagos → Suscripciones → Notificaciones → LicitAsist.

## 20. Estructura del proyecto

```
LicitaWatch/
├── api-usuarios/          # 8081 · auth, perfiles, roles, confirmación de correo, admin de usuarios
├── api-licitaciones/      # 8082 · licitaciones, postulaciones, búsqueda, límites de plan
├── api-ventas/            # 8083 · planes, suscripciones, ventas, pagos Webpay, voucher/PDF
├── api-notificaciones/    # 8084 · correos SMTP (plantilla HTML Thymeleaf), historial
├── api-asistente/         # 8085 · LicitAsist (Groq), sin BD
├── api-gateway/           # 8080 · Spring Cloud Gateway
│   └── src/main/java/cl/licitawatch/<servicio>/
│       controller · service · repository · entity · dto · security · config · exception · mapper · client
├── frontend/              # 5173 · React + Vite + TS + Tailwind
│   └── src/ api · store · types · utils · hooks · components/{ui,layout,charts} · pages/{public,pago,pyme,licitador,admin,shared}
├── postman/               # colección, environment, helper de pago sandbox
├── db/crear-bases.sql
├── scripts/               # start-all, stop-all, crear-bases, dev/ (generadores)
├── docs/
├── pom.xml                # agregador Maven
├── MEMORY.md · README.md · .env.example · .gitignore
```

## 21. Uso de `MEMORY.md`

`MEMORY.md` es la **memoria persistente** del proyecto: estado de cada microservicio (`[ ]` `[~]` `[x]` `[✓]` `[!]`), endpoints, migraciones, decisiones arquitectónicas (con su historial), bugs encontrados y solucionados, pruebas ejecutadas con sus resultados, sprints y próximos pasos. **Léelo antes de modificar el proyecto** y actualízalo después de cada cambio importante. Solo se marca `[✓]` lo que fue compilado, probado y verificado.

## 22. Estado actual del desarrollo

- ✅ 6 servicios backend compilados, con Flyway aplicado en PostgreSQL real y 32 pruebas JUnit/MockMvc en verde.
- ✅ Postman / Newman contra el sistema real: **422/422 aserciones OK**.
- ✅ Webpay Plus sandbox: transacción real creada en Transbank, confirmación server-side, anulación verificada.
- ✅ Groq real (LicitAsist) verificado; credenciales SMTP verificadas.
- ✅ Frontend: typecheck y build OK, revisado visualmente contra el backend y responsive verificado en 34 rutas.
- Detalle completo en `MEMORY.md`.

## 23. Problemas conocidos

- Flyway 10.10 (gestionado por Spring Boot 3.3) muestra un WARN porque PostgreSQL 18 es más nuevo que la versión soportada oficialmente. Funciona correctamente.
- El pago aprobado de extremo a extremo requiere un paso humano en el formulario de Transbank (sandbox).
- Los tokens JWT se guardan en `localStorage` del navegador (decisión de alcance del MVP). No hay refresh token: al expirar (60 min) se vuelve a iniciar sesión.
- El rate limiting es en memoria (una instancia de Gateway). Para varias instancias se necesitaría un backend distribuido para Bucket4j.

## 24. Próximos pasos

- Ejecutar el flujo humano completo: registro, correo real, pago sandbox y LicitAsist Premium.
- Renovación automática y cancelación de Premium; recordatorio de vencimiento por correo.
- Refresh tokens y cookies `HttpOnly`.
- Pruebas E2E automatizadas del frontend (Playwright) contra el backend real.
- Despliegue productivo con HTTPS y credenciales Webpay de producción.
