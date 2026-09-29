# LICITAWATCH — MEMORIA DEL PROYECTO

> Registro histórico permanente. Leer SIEMPRE antes de modificar el proyecto.
> Estados: `[ ]` Pendiente · `[~]` En desarrollo · `[x]` Implementado · `[✓]` Implementado y probado · `[!]` Problema detectado

## Estado general

- Fuente de verdad funcional: `LicitaWatch_Presentacion_ACTUALIZADA.pptx` (18 diapositivas) + `LicitaWatch_Modelo_ER.pdf` (5 páginas).
- Ejecución 100% on-premise en Windows 11 (sin Docker): Java 21.0.8, Maven 3.9.15, Node 24.20 / npm 11.19, PostgreSQL 18.6 nativo (`C:\Program Files\PostgreSQL\18\bin\psql.exe`, no está en PATH).
- PostgreSQL local: usuario `postgres`, contraseña `root` (configurada solo en `.env`, que está en .gitignore).
- ~~Fase actual: FASE 1 — API Usuarios.~~ (histórico)
- **Fase actual (2026-09-25): FASES 1-8 completadas y verificadas; FASE 9 (E2E) verificada por API/Newman y en navegador, salvo los pasos humanos** (clic en el correo real de confirmación, pago en el formulario Webpay sandbox, LicitAsist como Premium real).
- Arranque: `scripts\start-all.ps1` (backend + frontend) / `scripts\stop-all.ps1`.

### Criterios de finalización (sec. 57)
- [✓] PostgreSQL funciona · [✓] Flyway funciona (4 BDs, 22 migraciones)
- [✓] API Usuarios · [✓] API Licitaciones · [✓] API Ventas · [✓] API Notificaciones · [✓] API Asistente · [✓] Gateway · [✓] Frontend · [✓] Diseño tecnológico/moderno
- [✓] Registro (API + JUnit) · [~] Confirmación de email: lógica probada (JUnit + token real en PG); el clic desde un correo real lo debe hacer el usuario · [✓] Login · [✓] JWT · [✓] Roles
- [✓] Licitaciones · [✓] Postulaciones · [✓] Plan Estándar · [~] Plan Premium: activación probada en JUnit con commit simulado; activación con un pago sandbox real pendiente del usuario
- [~] Webpay: create real y retorno anulado verificados en vivo; pago aprobado real pendiente del usuario · [✓] Confirmación de pago (server-side, JUnit) · [✓] Voucher (JSON/PDF/correo en JUnit) · [✓] Historial de pagos
- [✓] Emails: SMTP Gmail autenticado, envíos reales durante Newman (alias licitawatch+tag@gmail.com), plantillas verificadas con GreenMail
- [✓] Admin · [✓] LicitAsist (Groq real) · [✓] Administrador usa LicitAsist · [~] Pyme/Licitador Premium usan LicitAsist (regla probada en JUnit; en vivo requiere Premium real)
- [✓] Postman: todos los endpoints, positivos, negativos, JSON importable, variables y scripts (Newman 422/422)
- [✓] JUnit · [✓] MockMvc · [✓] Seguridad (401/403/429/SQLi/CORS)
- [✓] README completo · [✓] .env.example · [✓] Credenciales admin documentadas · [✓] MEMORY.md actualizado

## Arquitectura actual

PPT diap. 10-11: Frontend React+TS → **API Gateway (8080)** [JWT, RBAC, CORS, Rate limit, SQL injection] → microservicios REST, cada uno con su propia BD PostgreSQL. Sin bus de mensajería: Licitaciones y MS-Ventas llaman a Notificaciones por REST directo.

| Componente | Puerto | BD |
|---|---|---|
| API Gateway | 8080 | — |
| API Usuarios | 8081 | licitawatch_usuarios |
| API Licitaciones | 8082 | licitawatch_licitaciones |
| API Ventas (MS-Ventas) | 8083 | licitawatch_ventas |
| API Notificaciones | 8084 | licitawatch_notificaciones |
| API Asistente (LicitAsist) | 8085 | sin BD (orquestador) |
| Frontend | 5173 | — |

## Microservicios implementados

(ver "Estado de cada microservicio")

## Estado de cada microservicio

- [✓] API Usuarios (8081) — compilado, Flyway V1-V7 aplicado en PostgreSQL real, 8 tests JUnit OK, smoke test manual OK (2026-09-25)
- [✓] API Licitaciones (8082) — compilado, Flyway V1-V6 en PostgreSQL real, 5 tests de integración OK, smoke test manual OK (crear borrador valida licitador por REST contra Usuarios). [~] Integración real con Ventas (plan) y Notificaciones pendiente de verificar cuando existan (FASE 3/4).
- [✓] API Ventas (8083) — compilado, Flyway V1-V7 en PostgreSQL real, 4 tests OK, smoke test real: planes, Estándar automático, venta Premium, **transacción creada en Transbank integración real**, confirmación sin pago → 409, retorno abortado → ANULADO. Integración Licitaciones→Ventas (/internal plan) verificada en vivo. [ ] Pago aprobado real en formulario Webpay: lo debe completar el usuario en el navegador (tarjetas de prueba de Transbank).
- [✓] API Notificaciones (8084) — compilado, Flyway V1-V2 en PostgreSQL real, 4 tests OK (SMTP real contra GreenMail), credenciales SMTP Gmail verificadas (login STARTTLS OK, sin enviar). [ ] Envío real a un buzón: pendiente de E2E con un email que indique el usuario.
- [✓] API Asistente / LicitAsist (8085, sin BD) — compilado, 4 tests OK, prueba en vivo: PYME Estándar → 403 PREMIUM_REQUERIDO, sin token → 401, ADMINISTRADOR → respuesta real de Groq (openai/gpt-oss-120b, ~4 s) con datos reales y exactos de la plataforma. [ ] PYME/LICITADOR Premium en vivo: pendiente de un pago Webpay sandbox completado en el navegador.
- [✓] API Gateway (8080) — Spring Cloud Gateway 2023.0.6, 7 tests OK, verificado en vivo contra los 5 servicios: enrutamiento 200, 401 (sin/invalid/expirado), 403 admin con pyme, 400 SQLi (incluida doble codificación), 404 /internal, CORS solo localhost:5173 (evil → 403), 429 tras 30 logins/min con Retry-After.
- [✓] Frontend (5173) — ver sección Frontend.

## Endpoints implementados

### API Usuarios (8081) [✓]
Públicos:
- `POST /api/auth/registro/pyme` {email,password,confirmPassword,rut,nombreContacto} → 201
- `POST /api/auth/registro/licitador` {email,password,confirmPassword,rut,razonSocial} → 201
- `POST /api/auth/confirmar-email` {token} · `GET /api/auth/confirmar-email?token=`
- `POST /api/auth/reenviar-confirmacion` {email} (respuesta genérica)
- `POST /api/auth/login` {email,password} → {token,tipo,expiraEn,usuario}
Autenticados:
- `GET /api/usuarios/me` · `PUT /api/usuarios/me` · `PUT /api/usuarios/me/password`
ADMINISTRADOR:
- `GET /api/admin/usuarios?rol&activo&q&page&size` · `GET /api/admin/usuarios/{id}` · `POST /api/admin/usuarios` · `PUT /api/admin/usuarios/{id}`
- `PATCH /api/admin/usuarios/{id}/estado` {activo} · `PATCH /api/admin/usuarios/{id}/rol` {rol,rut,...}
- `GET /api/admin/roles` · `GET /api/admin/usuarios/estadisticas`
Internos (X-Internal-Key):
- `GET /internal/usuarios/{id}` · `GET /internal/usuarios?ids=` · `GET /internal/perfiles/licitadores/{id}` · `GET /internal/perfiles/licitadores?ids=` · `GET /internal/perfiles/pymes/{id}` · `GET /internal/perfiles/pymes?ids=`
Swagger: http://localhost:8081/swagger-ui.html

### API Licitaciones (8082) [✓ JUnit]
- `GET /api/licitaciones?q&categoria&region&montoMin&montoMax&cierreDesde&cierreHasta&orden&page&size` (no-admin ve solo PUBLICADA; Premium destacadas primero; PYME con filtros cuenta búsqueda)
- `GET /api/licitaciones/mias` (LICITADOR) · `GET /api/licitaciones/resumen` (KPIs por rol) · `GET /api/licitaciones/uso-plan`
- `GET /api/licitaciones/{id}` · `POST /api/licitaciones` (LICITADOR, publicar=true|false) · `PUT /api/licitaciones/{id}` (dueño) · `PATCH /api/licitaciones/{id}/estado` (dueño) · `DELETE /api/licitaciones/{id}` (dueño, 409 si tiene postulaciones)
- `GET /api/licitaciones/{id}/postulaciones` (dueño/admin) · `POST /api/licitaciones/{id}/postulaciones` (PYME)
- `GET /api/postulaciones/mias` (PYME) · `GET /api/postulaciones/{id}` · `PUT /api/postulaciones/{id}` (PYME, ENVIADA) · `PATCH /api/postulaciones/{id}/retirar` (PYME) · `PATCH /api/postulaciones/{id}/estado` (LICITADOR: EN_REVISION|RECHAZADA) · `POST /api/postulaciones/{id}/seleccionar` (LICITADOR → ADJUDICADA, resto RECHAZADA)
- Admin: `GET /api/admin/licitaciones` · `GET /api/admin/licitaciones/estadisticas` · `PATCH /api/admin/licitaciones/{id}/moderar` {accion SUSPENDER|REACTIVAR, motivo} · `DELETE /api/admin/licitaciones/{id}` · `GET /api/admin/postulaciones`
- Estados licitación: BORRADOR→PUBLICADA→CERRADA(→PUBLICADA reabrir)→ADJUDICADA; *→CANCELADA; SUSPENDIDA (admin). Cierre automático por fecha (scheduler horario + al arrancar).
- Llama a: Usuarios `/internal/perfiles/**` (valida licitador_id/pyme_id), Ventas `/internal/suscripciones/usuario/{id}/plan` (límites), Notificaciones `/internal/notificaciones` (async tras commit): LICITACION_PUBLICADA, POSTULACION_RECIBIDA, POSTULACION_ENVIADA, RESULTADO_POSTULACION, LICITACION_ADJUDICADA, GENERAL.
### API Ventas (8083) [✓ JUnit + smoke]
- Público: `GET /api/planes` · `GET /api/planes/{id}` · `GET /api/planes/nombre/{ESTANDAR|PREMIUM}` · `GET|POST /api/pagos/webpay/retorno` (Webpay → 302 a FRONTEND/pago/resultado?orden=&estado=)
- `GET /api/suscripciones/mi` (asigna Estándar si no hay; accesoLicitasist) · `GET /api/suscripciones/mis`
- `POST /api/ventas` {planId} (PYME/LICITADOR) · `GET /api/ventas/mias` · `GET /api/ventas/{id}`
- `POST /api/pagos/webpay/transacciones` {ventaId} → {token,url} · `POST /api/pagos/webpay/confirmar` {token} (commit server-side, idempotente, 409 PAGO_NO_COMPLETADO)
- `GET /api/pagos/historial` · `GET /api/pagos/orden/{numeroOrden}` · `GET /api/pagos/{id}` · `GET /api/pagos/{id}/comprobante` (409 si no APROBADO) · `GET /api/pagos/{id}/comprobante/pdf` · `POST /api/pagos/{id}/comprobante/reenviar`
- Admin: `GET /api/admin/ventas` · `GET /api/admin/ventas/estadisticas` · `GET /api/admin/pagos?estado` · `GET /api/admin/suscripciones?estado&plan&usuarioId`
- Interno: `GET /internal/suscripciones/usuario/{usuarioId}/plan`

### API Notificaciones (8084) [✓ JUnit]
- Interno: `POST /internal/notificaciones` {usuarioId, tipo, destinatario, datos, adjuntos[{nombre,contentType,base64}]} → {id, estado ENVIADO|FALLIDO}
- `POST /api/notificaciones` (ADMIN) {usuarioId, asunto, mensaje} → valida usuario por REST y envía GENERAL
- `GET /api/notificaciones/mias` · `GET /api/notificaciones/mias/no-leidas` · `PATCH /api/notificaciones/mias/leidas` · `GET /api/notificaciones/{id}` (confirmar envío) · `PATCH /api/notificaciones/{id}/leida`
- Admin: `GET /api/admin/notificaciones?tipo&estado&usuarioId` · `GET /api/admin/notificaciones/estadisticas`
- Tipos: CONFIRMACION_REGISTRO, LICITACION_PUBLICADA, POSTULACION_RECIBIDA, POSTULACION_ENVIADA, RESULTADO_POSTULACION, LICITACION_ADJUDICADA, PAGO_CONFIRMADO (voucher + PDF adjunto), PAGO_RECHAZADO, GENERAL.
- Plantilla única responsive `templates/email/base.html` (Thymeleaf, escape automático) con marca LicitaWatch, badge de estado, tabla de detalles, botón, enlace alternativo y aviso.

### API Asistente (8085) [✓]
- `GET /api/asistente/estado` → {acceso, rol, plan, motivo, modelo, sugerencias[6]}
- `POST /api/asistente/chat` {mensaje ≤2000, historial[≤12 {rol user|assistant, contenido}], licitacionId?} → {respuesta (markdown), modelo, rol, fuentes[], generadoEn}
- Errores: 401 sin/invalid JWT · 403 PREMIUM_REQUERIDO · 400 validación · 429 ASISTENTE_SATURADO (Groq) · 502/503 ASISTENTE_ERROR/NO_DISPONIBLE · 503 si Ventas no responde (no concede acceso por defecto).

### API Gateway (8080) [✓]
Rutas (sin reescritura de path): usuarios `/api/auth/**, /api/usuarios/**, /api/admin/usuarios/**, /api/admin/roles/**` · licitaciones `/api/licitaciones/**, /api/postulaciones/**, /api/admin/licitaciones/**, /api/admin/postulaciones/**` · ventas `/api/planes/**, /api/suscripciones/**, /api/ventas/**, /api/pagos/**, /api/admin/ventas/**, /api/admin/pagos/**, /api/admin/suscripciones/**` · notificaciones `/api/notificaciones/**, /api/admin/notificaciones/**` · asistente `/api/asistente/**`. `/internal/**` nunca se enruta (404) y el header `X-Internal-Key` del cliente se elimina.
Públicas: registro, login, confirmar-email, reenviar-confirmación, GET /api/planes/**, /api/pagos/webpay/retorno, /actuator/health.

- Contacto Licitador↔Pyme (sec. 40): sin microservicio nuevo. El licitador ve email/RUT de los postulantes; la pyme SELECCIONADA recibe el email del licitador (en la respuesta y en el correo de resultado).

## Base de datos

- [✓] 4 bases creadas con `db/crear-bases.sql` (2026-09-25): licitawatch_usuarios, licitawatch_licitaciones, licitawatch_ventas, licitawatch_notificaciones.
- **Regla del usuario: "CREAR BD TAL CUAL SALE EN LA PRESENTACIÓN".** Las tablas del modelo de la PPT se crean con exactamente esas columnas, tipos (`enum` → tipo ENUM nativo PostgreSQL, `date`, `numeric`, `bool`) y restricciones PK/FK/UNIQUE. Los datos técnicos que la PPT no modela (hash de contraseña, tokens, descripción, detalle Webpay...) van en **tablas de soporte 1:1 separadas**, sin alterar las tablas oficiales.

## Migraciones Flyway

### api-usuarios [✓ aplicadas en PostgreSQL 2026-09-25]
- V1__create_rol.sql (tipo ENUM rol_nombre + seed 3 roles) · V2__create_usuario.sql · V3__create_licitador.sql · V4__create_pyme.sql · V5__create_administrador.sql → tablas oficiales PPT.
- V6__create_cuenta_usuario.sql (soporte: password_hash BCrypt, email_confirmado, nombre, fechas).
- V7__create_token_verificacion.sql (soporte: hash SHA-256 del token, expira_en, usado_en).
### api-licitaciones [✓ aplicadas en PostgreSQL 2026-09-25]
- V1__create_licitacion.sql (ENUM licitacion_estado) · V2__create_postulacion.sql (ENUM postulacion_estado, FK licitacion) → tablas oficiales PPT.
- V3__create_licitacion_detalle.sql · V4__create_postulacion_detalle.sql · V5__create_busqueda_registro.sql (soporte) · V6__create_indices.sql (UNIQUE licitacion_id+pyme_id, índices).
### api-ventas [✓ aplicadas en PostgreSQL 2026-09-25]
- V1__create_plan_suscripcion.sql (ENUM plan_nombre ESTANDAR/PREMIUM + seed) · V2__create_suscripcion.sql (ENUM suscripcion_estado) · V3__create_venta.sql · V4__create_pago.sql (ENUM pago_estado, venta_id UNIQUE) → tablas oficiales PPT.
- V5__create_plan_detalle.sql (precio $0 / $24.990, límites Estándar 3 publicaciones, 5 postulaciones, 50 búsquedas/mes; Premium ilimitado, LicitAsist, visibilidad, 30 días) · V6__create_suscripcion_vigencia.sql · V7__create_pago_webpay.sql (buy_order, token, authorization_code, card_last_digits (solo 4), etc.).
### api-notificaciones [✓ aplicadas en PostgreSQL 2026-09-25]
- V1__create_notificacion.sql (ENUM notificacion_tipo, ENUM notificacion_canal 'EMAIL') → tabla oficial PPT.
- V2__create_notificacion_envio.sql (soporte: destinatario, asunto, resumen, estado ENVIADO/FALLIDO, error, leída, fechas). No guarda el cuerpo (puede traer tokens).

- Nota: Flyway 10.10 (gestionado por Boot 3.3) muestra WARN "PostgreSQL 18.6 is newer than this version" — funciona correctamente.

## Entidades

Modelo oficial PPT (diap. 12-15):
- Usuarios: `rol(id PK, nombre enum)`, `usuario(id PK, email UNIQUE, rol_id FK→rol, activo bool)`, `licitador(id PK, usuario_id FK/UNIQUE, razon_social texto, rut UNIQUE)`, `pyme(id PK, usuario_id FK/UNIQUE, nombre_contacto texto, rut UNIQUE)`, `administrador(id PK, usuario_id FK/UNIQUE, area texto)`.
- Licitaciones: `licitacion(id PK, licitador_id ref.Usuarios, titulo texto, monto_estimado numeric, fecha_cierre date, estado enum)`, `postulacion(id PK, licitacion_id FK, pyme_id ref.Usuarios, fecha_postulacion date, estado enum)`.
- Ventas: `plan_suscripcion(id PK, nombre Estándar/Premium)`, `suscripcion(id PK, usuario_id ref.Usuarios, plan_id FK, estado enum)`, `venta(id PK, suscripcion_id FK, monto numeric, fecha date)`, `pago(id PK, venta_id FK/UNIQUE, id_transaccion, pasarela, estado enum)`.
- Notificaciones: `notificacion(id PK, usuario_id ref.Usuarios, tipo enum, canal email)`.

## Relaciones

- rol 1:N usuario · usuario 1:1 licitador / pyme / administrador.
- licitacion 1:N postulacion.
- plan 1:N suscripcion · suscripcion 1:N venta · venta 1:1 pago.
- Referencias entre microservicios (licitador_id, pyme_id, usuario_id) NO son FK: se validan por API REST.

## Seguridad

- API Usuarios [✓]: Spring Security stateless, BCrypt(12), JWT filter, `/internal/**` con X-Internal-Key (comparación en tiempo constante), `/api/admin/**` solo ADMINISTRADOR, errores JSON uniformes 401/403 (códigos NO_AUTENTICADO, TOKEN_INVALIDO, TOKEN_EXPIRADO, ACCESO_DENEGADO), GlobalExceptionHandler sin stack traces, Bean Validation + validador @Rut (módulo 11), login con hash dummy para igualar tiempos (anti-enumeración), reenviar-confirmación con respuesta genérica, admin no puede desactivarse ni cambiarse el rol a sí mismo. Servicios escuchan en 127.0.0.1.

- API Gateway [✓]: GatewaySecurityFilter (orden: /internal→404, SQLi→400, rate limit→429, JWT→401, RBAC→403 para /api/admin/** y roles desconocidos), CorsWebFilter (solo FRONTEND_URL), GatewayErrorHandler (JSON sin stack traces; servicio caído → 503), rate limit Bucket4j 8.10.1 + Caffeine: AUTH 30/min por IP, ASISTENTE 20/min por usuario, GENERAL 300/min por usuario/IP (ventana fija refillIntervally).

## JWT

- HS512 (secreto de 64 bytes en JWT_SECRET), emitido por API Usuarios. Claims: `sub`=usuarioId, `email`, `rol`, `perfilId` (id en licitador/pyme/administrador), `nombre`, `iss`=licitawatch-api-usuarios, `exp` = JWT_EXPIRATION minutos (60).
- Todos los servicios validan con el mismo JWT_SECRET.

## Roles y permisos

LICITADOR, PYME, ADMINISTRADOR.

## Frontend

[~] `frontend/` React 18.3 + Vite 5.4 + TypeScript 5.6 + Tailwind 3.4 + React Router 6 + Zustand 4.5 + Axios 1.7 + recharts + lucide-react + react-markdown/remark-gfm + @fontsource-variable/inter. Puerto 5173 (strictPort). Lee `VITE_API_BASE_URL` del `.env` raíz (envDir '..'). Alias `@` → src.
- [✓] Verificado 2026-09-25: `tsc -b --noEmit` OK, `vite build` OK (chunks react/charts/markdown). Revisión visual en navegador contra el backend real: landing, dashboard Licitador (KPIs + gráficos), gestión de licitación con postulante, LicitAsist bloqueado para Estándar, panel Admin con estadísticas, LicitAsist Admin con respuesta real de Groq (markdown/tablas/fuentes), búsqueda y postulaciones Pyme en mobile, planes y pago. Prueba automática de desborde horizontal a 375 px en 34 rutas (públicas, Pyme, Licitador, Admin): todas OK tras corregir /pago, landing, tabla mobile y tarjetas de roles.
- Nota de verificación: el agente NO escribe contraseñas en formularios del navegador; para revisar dashboards cargó en localStorage un JWT obtenido por API para las cuentas demo. Registro por formulario, confirmación desde el correo y pago Webpay en el formulario de Transbank quedan para el usuario (pasos humanos).
- Auth (sec. 39): store Zustand persistido (`licitawatch-auth`: token, expiraEn, usuario, rol, suscripción). Axios interceptors: adjunta JWT; 401 → logout + redirect /login?expirada=1; 403 ACCESO_DENEGADO / 429 / red → toast. ProtectedRoute por sesión y rol.
- Pago: /pago crea venta + transacción y hace POST de formulario (token_ws) a Webpay; /pago/resultado consulta la orden al backend (nunca activa Premium en el frontend) y muestra voucher.

## Rutas frontend

Públicas: `/`, `/login`, `/register`, `/register/pyme`, `/register/licitador`, `/confirm-email?token=`, `/planes`. Pago: `/pago?plan=` (PYME/LICITADOR), `/pago/resultado?orden=&estado=`.
Dashboard: `/dashboard` (redirige por rol) · PYME `/dashboard/pyme`, `/dashboard/pyme/licitaciones`, `/dashboard/pyme/licitaciones/:id`, `/dashboard/pyme/postulaciones` · LICITADOR `/dashboard/licitador`, `/dashboard/licitador/licitaciones`, `/…/nueva`, `/…/:id`, `/…/:id/editar` · ADMIN `/dashboard/admin`, `/…/usuarios`, `/…/licitaciones`, `/…/postulaciones`, `/…/ventas`, `/…/notificaciones` · Compartidas `/dashboard/suscripcion`, `/dashboard/pagos`, `/dashboard/pagos/:id/comprobante`, `/dashboard/asistente?licitacion=`, `/dashboard/perfil`, `/dashboard/notificaciones`. 404 propio.

## Componentes frontend

`components/ui`: Button, Input (toggle contraseña), Textarea, Select, Card/CardHeader, Badge/StatusBadge, Modal (Esc, portal, bottom-sheet en mobile), ConfirmDialog, Table (tabla en desktop → tarjetas en mobile), Pagination, States (Spinner, Skeleton, LoadingCards, EmptyState, ErrorState, ForbiddenState, UnauthorizedState, NotFoundState), Toaster, KpiCard, ChartCard, SearchBar, FilterPanel, PageHeader, Logo, Tabs. Layout: DashboardLayout, Sidebar (por rol), Topbar (campana de notificaciones + menú perfil), AuthLayout. Dominio: LicitacionCard, UsoPlanCard, LicitAsistPromo, PostulacionModal, Voucher, charts EstadoDonut / BarrasMes. Hooks: useAsync, useSuscripcion.

## Diseño e identidad visual

Marca LicitaWatch (logo "L" + punto, degradado azul eléctrico → índigo). Paleta `brand` (azules) + `ink` (azul noche) en tailwind.config. Tipografía Inter Variable. Landing/auth/sidebar oscuros con grilla sutil y glow; contenido de dashboards claro con cards de sombra suave. Microanimaciones (fade/slide), estados hover/focus visibles (ring), glassmorphism moderado solo en landing. Responsive: sidebar off-canvas en mobile, tablas → tarjetas, modales bottom-sheet.

## Integraciones externas

## Webpay

**Webpay Plus en SANDBOX** (indicación del usuario 2026-09-25): ambiente de integración de Transbank, comercio de pruebas 597055555532, URL webpay3gint.transbank.cl, tarjetas de prueba, sin cobros reales. `WEBPAY_ENVIRONMENT=SANDBOX` (alias de INTEGRATION; cualquier valor distinto de PRODUCTION usa sandbox).

[✓] Transbank SDK Java 6.2.1 (`TransbankWebpayGateway` implementa el puerto `WebpayGateway`). Ambiente INTEGRATION (credenciales públicas 597055555532). Verificado en vivo: `create` real devuelve token + `https://webpay3gint.transbank.cl/webpayserver/initTransaction`.
Flujo: POST /api/ventas (venta+pago+suscripción PENDIENTE, buy_order `LW-yyMMddHHmmss-{ventaId}`) → POST /api/pagos/webpay/transacciones (create) → navegador POST a url con token_ws → Webpay vuelve a WEBPAY_RETURN_URL (Gateway 8080 → Ventas) → backend hace `commit` → valida AUTHORIZED + responseCode 0 + monto + orden → APROBADO: activa Premium 30 días (cancela Estándar), evento → correo PAGO_CONFIRMADO con voucher HTML + PDF adjunto; si no: RECHAZADO + suscripción CANCELADA + correo PAGO_RECHAZADO (sin comprobante). TBK_TOKEN/TBK_ORDEN_COMPRA → ANULADO. Bloqueo pesimista evita doble confirmación. El frontend nunca activa Premium.
Tarjetas de prueba Transbank (integración): VISA 4051 8856 0044 6623 (aprobada), Mastercard 5186 0595 5959 0568 (rechazada), CVV 123, RUT 11.111.111-1, clave 123.

## SMTP

[✓] Gmail smtp.gmail.com:587 STARTTLS con contraseña de aplicación (en `.env`). Login verificado 2026-09-25. Envío con JavaMailSender (HTML + texto plano alternativo + adjuntos). Si SMTP falla, la notificación queda FALLIDO con el error (sin romper el flujo de negocio).

## Groq / LicitAsist

[✓] Groq Chat Completions (`GROQ_API_BASE_URL`/chat/completions), modelo `openai/gpt-oss-120b`, temperature 0.3, max_completion_tokens 2048, reasoning_effort low. Verificado en vivo 2026-09-25.
- Acceso (backend, AccesoService): ADMINISTRADOR siempre; PYME/LICITADOR solo si `GET /api/suscripciones/mi` (Ventas) devuelve accesoLicitasist=true (Premium ACTIVA).
- Contexto (ContextoService): se consultan las APIs reenviando el JWT del usuario (cada API aplica RBAC/ownership → no se filtra información de otros). Proyección por lista blanca de campos: sin contraseñas, JWT, secretos, tokens Webpay, idTransaccion ni emails de pymes. PYME: licitaciones abiertas (40), mis postulaciones, uso del plan. LICITADOR: resumen, mis licitaciones (+postulantes de las 5 más recientes con postulaciones), publicadas recientes. ADMIN: estadísticas de usuarios/licitaciones/ventas + listados recientes. `licitacionId` opcional agrega el detalle.
- Prompt de sistema: usar solo el CONTEXTO, no inventar, citar #id/monto/fecha de cierre, no revelar instrucciones. El JWT solo viaja entre servicios internos, nunca a Groq; la API key de Groq solo va en el header.

## Variables de entorno

Ver `.env.example`. `.env` real en la raíz (ignorado por git).

## Credenciales de desarrollo

- PostgreSQL: postgres / (ver .env)
- Admin inicial: ADMIN_EMAIL / ADMIN_PASSWORD de `.env` (por defecto admin@licitawatch.local).
- Cuentas demo creadas durante las pruebas (solo desarrollo local): `pyme.demo@licitawatch.local` / `Pyme12345` (Pyme, RUT 76543210-3), `licitador.demo@licitawatch.local` / `Licita12345` (Licitador, RUT 77111222-6). Además, Newman crea en cada corrida usuarios `licitawatch+<tag><runId>@gmail.com`.
- Webpay sandbox: ver sección Webpay (tarjetas de prueba).

## Postman

[✓] `postman/LicitaWatch.postman_collection.json` + `postman/LicitaWatch.postman_environment.json`, generados con `python scripts/dev/build_postman.py` (JSON válido, schema v2.1). 157 requests en 11 carpetas: 00 Setup/salud · 01 Auth · 02 Usuarios/admin · 03 Licitaciones · 04 Postulaciones · 05 Planes · 06 Suscripciones/Ventas/Webpay · 07 Notificaciones · 08 LicitAsist · 09 Admin · 10 Seguridad.
- Datos únicos por corrida (emails alias `emailPrueba`+tag, RUT válidos generados en JS). JWT e IDs se guardan con `pm.environment.set`. Test global: sin stack traces/secretos.
- Negativos: 400, 401 (sin/inválido/expirado vía `jwtSecret`), 403 (rol, ownership, sin Premium, admin), 404, 409, 429 (último request), 5xx (formato controlado).
- Pasos manuales (se omiten automáticamente si no aplican): `confirmToken` (token del correo), `pagoCompletado=si` tras pagar en sandbox con `postman/pagar-webpay.html?token=...`, chats Pyme/Licitador Premium.
- **Ejecución Newman 2026-09-25 contra el sistema real: 422/422 aserciones OK, 0 fallos** (`npx newman run ... --env-var jwtSecret=...`). Primera corrida: 2 fallos por dato de prueba (nombreContacto < 3 caracteres) → corregido.
- [✓] 2026-09-25 (pedido del usuario, para evidencias): `postman/LicitaWatch-Evidencias.postman_collection.json` — mismas 157 requests y pruebas, pero con los datos enviados LITERALES y visibles en URL/body (emails, RUT, contraseñas de prueba, fechas). Solo IDs del servidor y JWT siguen como variables. Además registra en la consola de Postman cada request y response. Generada por el mismo `build_postman.py` con un sello (MMddHHmm) → cada generación trae datos nuevos; una colección ya ejecutada da 409 al repetirse (regenerar). Newman: 422/422 OK (sello 09251807). Entregada sin usar: sello 09251808.
- Fix 2026-09-25: el test 429 fallaba si la ventana del rate limit se renovaba durante los 35 intentos → ahora el pre-request envía intentos hasta ver el primer 429 (máx. 100).
- Nota: la corrida envía correos reales vía Gmail a alias `licitawatch+tag@gmail.com` (llegan al buzón de pruebas).

## Tests

- api-usuarios [✓] `mvn test` → 8/8 OK (2026-09-25): RutUtilsTest (2), AuthFlowIntegrationTest (3: registro→correo→confirmación→login, validaciones/duplicados, credenciales inválidas), AdminSecurityIntegrationTest (3: CRUD admin + roles + desactivar, 401 sin/invalid token, /internal con API key). H2 modo PostgreSQL, Flyway desactivado en tests, NotificacionClient simulado solo en el borde HTTP.
- api-licitaciones [✓] `mvn test` → 5/5 OK (2026-09-25): LicitacionesIntegrationTest (flujo publicar→buscar→postular→duplicado 409→revisar→seleccionar→ADJUDICADA/RECHAZADA + notificaciones; ownership 403 y transiciones de estado; validaciones 400/401/403/404; límites plan Estándar 403 LIMITE_PLAN y Premium ilimitado+destacada primero; admin moderar/listar/estadísticas/eliminar). Bordes HTTP (Usuarios, Ventas, Notificaciones) simulados con @MockBean; JPA/servicios/seguridad reales.
- api-ventas [✓] `mvn test` → 4/4 OK (2026-09-25): VentasWebpayIntegrationTest (aprobado → Premium + historial + comprobante + PDF + correo con adjunto + idempotencia + 409 PLAN_VIGENTE + /internal plan; rechazado → sin Premium, 409 comprobante, correo PAGO_RECHAZADO; commit sin pago → 409 PAGO_NO_COMPLETADO y monto alterado → RECHAZADO; seguridad 401/403/400 + admin). Transbank simulado solo en el borde (WebpayGateway @MockBean).
- api-notificaciones [✓] `mvn test` → 4/4 OK (2026-09-25): NotificacionesSmtpIntegrationTest con GreenMail (servidor SMTP real en memoria): correo de confirmación (botón, enlace, expiración, escape XSS), voucher con PDF adjunto, seguridad/admin/validaciones, SMTP caído → FALLIDO.
- api-asistente [✓] `mvn test` → 4/4 OK (2026-09-25): Estándar → 403 sin llamar a Groq; Premium → respuesta + verificación de que a Groq NO llegan tokens/idTransaccion/emails/JWT; admin sin consultar suscripción; 401/400 (rol 'system' en historial rechazado)/503.
- api-gateway [✓] `mvn test` → 7/7 OK (2026-09-25): GatewaySecurityIntegrationTest con microservicio simulado (HttpServer embebido): enrutamiento + JWT reenviado + X-Internal-Key eliminado, rutas públicas, 401 (ausente/inválido/expirado/firma falsa), RBAC 403, SQLi 400 + /internal 404, 429 con Retry-After, CORS.
- Smoke test manual en PostgreSQL real (curl) OK: login admin, registro pyme, 409 email/RUT, 400 validación, 403 sin confirmar, confirmación, login pyme, /me, 403 admin con pyme, 401, admin crea licitador, listar/filtrar, roles, estadísticas, /internal.

## Bugs encontrados

- [!→✓] 2026-09-25: `.env` se lee como ISO-8859-1 → ADMIN_AREA "Administración" quedó como "AdministraciÃ³n".
- [!] 2026-09-25 (entorno del agente, no del proyecto): la JVM no pudo abrir su pipe loopback (AF_UNIX) dentro del sandbox de la terminal del agente ("Unable to establish loopback connection"). Se evita con `-Djdk.net.unixdomain.tmpdir=C:/lwtmp`. En el PC del usuario no ocurre; los scripts de arranque igual incluyen la opción por seguridad.

## Bugs solucionados

- 2026-09-25: `scripts/start-all.ps1` — con TEMP en una ruta con espacios (`C:\Users\Francisca Araya Soto\...`) la JVM fallaba con "Unable to establish loopback connection". Ahora usa `-Djdk.net.unixdomain.tmpdir=C:\licitawatch-tmp` (fallback `.run\tmp`). Verificado: los 6 servicios arrancan OK con el script.
- 2026-09-25: Frontend — desborde horizontal en mobile en /pago, landing (halo 900 px), tablas mobile con emails largos y tarjetas de roles admin. Corregido (min-w-0, overflow-hidden, overflow-wrap:anywhere, grid responsive).
- 2026-09-25: Aclaración — el "AdministraciÃ³n" observado después del fix era un artefacto de lectura del agente (Python con cp1252); en BD el valor es UTF-8 correcto (hex c3b3) y la API devuelve "Administración".
- 2026-09-25: Gateway — `refillGreedy` reponía tokens continuamente (con logins de ~300 ms nunca llegaba a 429). Cambiado a `refillIntervally` (ventana fija por minuto).
- 2026-09-25: Gateway — SQLi con doble codificación (%2527, %2520) evadía el filtro. Ahora se decodifica iterativamente (hasta 3 veces).
- 2026-09-25: Gateway — CORS con `globalcors` + placeholder era frágil; reemplazado por `CorsWebFilter` explícito (CorsConfig).
- 2026-09-25: API Notificaciones — `MimeMessageHelper` sin modo multipart fallaba al enviar HTML + texto alternativo ("Not in multipart mode"). Detectado por los tests con GreenMail; corregido usando siempre multipart=true.
- 2026-09-25: caracteres no ASCII en `.env` → usar escape Unicode (`ADMIN_AREA=Administraci\u00f3n`); documentado en .env.example. Registro corregido en BD.

## Problemas pendientes

- [ ] Pasos humanos del E2E (ver Estado general).
- [ ] `mvn test` desde el pom raíz en una sola corrida: no ejecutado (el usuario canceló ese comando); cada módulo pasó por separado.
- [ ] Flyway 10.10 + PostgreSQL 18: WARN de versión no probada oficialmente (funciona).
- [ ] Mejoras futuras: refresh tokens/cookies HttpOnly, renovación automática de Premium, E2E automatizado del frontend (Playwright).

## Decisiones arquitectónicas

- **2026-09-25 — Reinicio del proyecto.** El repositorio tenía una versión anterior (commit `fd4bb93`, carpetas `backend/`, `gateway/`, modelo empresa/cliente) que fue eliminada del directorio de trabajo por el usuario y NO coincide con la PPT actualizada (licitador/pyme). Se construye desde cero con la estructura del prompt maestro. La versión anterior sigue recuperable en el historial de git.
- Estructura raíz: `api-usuarios/ api-licitaciones/ api-ventas/ api-notificaciones/ api-asistente/ api-gateway/ frontend/ postman/ docs/ db/ scripts/`.
- Maven (un `pom.xml` agregador en la raíz + un pom independiente por servicio). Spring Boot 3.3.13.
- ENUM nativos de PostgreSQL + parámetro JDBC `stringtype=unspecified` para que Hibernate envíe los enums como texto y PostgreSQL los convierta al tipo ENUM (así las entidades usan `@Enumerated(STRING)` y los tests en H2 funcionan con VARCHAR).
- Comunicación interna: endpoints `/internal/**` protegidos con header `X-Internal-Key` (= `INTERNAL_API_KEY`). El Gateway nunca enruta `/internal/**`. Los microservicios escuchan en 127.0.0.1.
- Cada microservicio valida el JWT por sí mismo (defensa en profundidad) además del Gateway.

## Cambios realizados

### 2026-09-25

- Análisis de la PPT (18 diapositivas) y del PDF del modelo ER.
- `.env`: POSTGRES_USER=postgres, POSTGRES_PASSWORD=root; agregadas variables RATE_LIMIT_*.
- Creados `.gitignore`, `.env.example`, `db/crear-bases.sql`, `MEMORY.md`.
- Creadas las 4 bases en PostgreSQL local (verificado con psql).
- **FASE 1 API Usuarios [✓]**: `api-usuarios/` (pom Boot 3.3.13, JJWT 0.12.6, springdoc 2.6.0, Lombok, H2 test). Paquetes controller/service/repository/entity/dto/security/config/exception/mapper/client/util/validation. Archivos clave: AuthController, UsuarioController, AdminUsuarioController, InternalUsuarioController, AuthService, EmailConfirmacionService, PerfilService, AdminUsuarioService, UsuarioService, JwtService, JwtAuthenticationFilter, InternalApiKeyFilter, SecurityConfig, AdminSeeder, NotificacionClient, RutUtils, GlobalExceptionHandler. BD licitawatch_usuarios modificada con V1-V7. Admin inicial creado desde .env (admin@licitawatch.local). El correo de confirmación depende de API Notificaciones (FASE 4): mientras no esté levantada, el registro responde `correoEnviado:false` y se puede reenviar.
- **FASE 2 API Licitaciones [✓]**: `api-licitaciones/` (mismo stack). Clases comunes copiadas con `scripts/dev/copy_common.py`. Archivos clave: LicitacionController, PostulacionController, AdminLicitacionController, LicitacionService, PostulacionService, PlanService (límites mensuales), ResumenService, AdminLicitacionService, LicitacionSpecs (Criteria API, sin SQL concatenado), CierreAutomaticoScheduler, UsuariosClient, VentasClient, NotificacionClient + NotificacionListener (@TransactionalEventListener AFTER_COMMIT + @Async). BD licitawatch_licitaciones con V1-V6. Nota: al probar con curl desde Git Bash los acentos se envían en cp1252 → 400; con archivo UTF-8 funciona (no es bug del código).
- **FASE 3 API Ventas [✓]**: `api-ventas/` (+ transbank-sdk-java 6.2.1, openpdf 2.0.3). Archivos clave: PlanController, SuscripcionController, VentaController, PagoController, AdminVentasController, InternalVentasController, PlanService, SuscripcionService (Estándar automático, vencimiento Premium), VentaService, PagoService (commit + validaciones), ComprobanteService, VoucherPdfGenerator, AdminVentasService, WebpayGateway/TransbankWebpayGateway, PagoNotificacionListener. BD licitawatch_ventas V1-V7.
- **FASE 4 API Notificaciones [✓]**: `api-notificaciones/` (+ spring-boot-starter-mail, thymeleaf; test: greenmail-junit5 2.0.1). Archivos clave: InternalNotificacionController, NotificacionController, AdminNotificacionController, NotificacionService, EmailContenidoFactory (textos por tipo), EmailSender (Thymeleaf + SMTP), UsuariosClient. BD licitawatch_notificaciones V1-V2.
- **FASE 5 API Asistente [✓]**: `api-asistente/` (sin JPA/Flyway/PostgreSQL). Archivos: AsistenteController, AsistenteService, AccesoService, ContextoService, GroqClient, PlataformaClient, SecurityConfig (solo /api/asistente/** con roles PYME/LICITADOR/ADMINISTRADOR).
- **FASE 6 API Gateway [✓]**: `api-gateway/` (spring-cloud-starter-gateway, jjwt, bucket4j-core 8.10.1, caffeine). Archivos: GatewaySecurityFilter, JwtValidator, RutasPublicas, SqlInjectionGuard, RateLimiterService, CorsConfig, GatewayErrorHandler, application.yml (rutas).
- Herramienta de desarrollo `scripts/dev/unbundle.py` (genera archivos desde un bundle de texto).

## Sprints (Scrum, PPT diap. 17: 8 sprints × 2 semanas)

- S1 Análisis y diseño: [✓] análisis PPT/ER, arquitectura definida.
- S2 Arquitectura inicial: [✓] estructura, BDs, convenciones (JWT compartido, /internal, errores JSON)
- S3 Usuarios y auth: [✓] API Usuarios
- S4 Licitaciones: [✓] API Licitaciones (CRUD, búsqueda, filtros, estados)
- S5 Postulaciones: [✓] postular, revisar, seleccionar, retirar
- S6 Ventas: [✓] planes, suscripciones, ventas
- S7 Pagos y pruebas: [✓] Webpay Plus + comprobante, Postman/Newman 422/422, 32 tests JUnit
- S8 Cierre y producción: [~] frontend, README, scripts on-premise [✓]; pendiente E2E humano y despliegue productivo

## Próximos pasos

1. ~~FASE 1 API Usuarios~~ [✓]
2. ~~FASE 2 API Licitaciones~~ [✓]
3. ~~FASE 3 API Ventas~~ [✓]
4. ~~FASE 4 API Notificaciones~~ [✓]
5. ~~FASE 5 API Asistente~~ [✓]
6. ~~FASE 6 API Gateway~~ [✓]
7. ~~FASE 7~~ [✓] pom agregador raíz (`pom.xml`), `scripts/start-all.ps1`, `scripts/stop-all.ps1`, `scripts/crear-bases.ps1`, Postman + Newman 422/422. [ ] Corrida única `mvn test` desde la raíz: no ejecutada (el usuario canceló el comando); cada módulo pasó por separado (8+5+4+4+4+7 = 32 tests).
8. ~~FASE 8 Frontend React~~ [✓]
9. FASE 9 E2E: [✓] backend completo vía Gateway (Newman 422/422) + frontend contra backend real. [ ] Pasos humanos pendientes de ejecutar por el usuario: registro desde el formulario → clic en el correo real de confirmación → pago Webpay sandbox con tarjeta de prueba → correo con voucher/PDF → LicitAsist como Pyme/Licitador Premium.
10. README completo.

## Última actualización

2026-09-25 — inicio del proyecto.
2026-09-25 — FASES 1-8 completas; README, docs/resumen-tecnico.md, Postman (422/422) y frontend verificados. Servicios corriendo con scripts/start-all.ps1.
