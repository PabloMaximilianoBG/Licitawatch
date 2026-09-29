# LicitaWatch — Resumen técnico (sección 59 del prompt maestro)

Fuente de verdad: `LicitaWatch_Presentacion_ACTUALIZADA.pptx` (18 diapositivas) y `LicitaWatch_Modelo_ER.pdf`.

## 1. Arquitectura
Microservicios Spring Boot independientes, cada uno con su propia BD PostgreSQL, detrás de un API Gateway (JWT, RBAC, CORS, rate limit, SQL injection). Frontend React + TypeScript. Comunicación REST, sin bus de mensajería (PPT diap. 10-11).

## 2-3. Microservicios y responsabilidades
| Servicio | Puerto | Responsabilidad |
|---|---|---|
| API Usuarios | 8081 | Registro por perfil, confirmación de correo, login JWT, perfiles, roles, administración de usuarios, admin inicial |
| API Licitaciones | 8082 | CRUD y estados de licitaciones, búsqueda/filtros, postulaciones, selección, límites de plan, moderación |
| API Ventas (MS-Ventas) | 8083 | Planes, suscripciones, ventas, pagos Webpay Plus, voucher/PDF, historial |
| API Notificaciones | 8084 | Correos SMTP por evento + historial (`notificacion`) |
| API Asistente | 8085 | LicitAsist: orquesta las APIs + Groq. Sin BD |
| API Gateway | 8080 | Punto único de entrada |
| Frontend | 5173 | React 18 + Vite + Tailwind + Zustand + Axios |

## 4-5. Entidades y relaciones (tal cual la PPT)
- `rol(id, nombre enum)` 1:N `usuario(id, email UNIQUE, rol_id FK, activo)`; `usuario` 1:1 `licitador(id, usuario_id FK/UNIQUE, razon_social, rut UNIQUE)`, `pyme(id, usuario_id FK/UNIQUE, nombre_contacto, rut UNIQUE)`, `administrador(id, usuario_id FK/UNIQUE, area)`.
- `licitacion(id, licitador_id ref, titulo, monto_estimado numeric, fecha_cierre date, estado enum)` 1:N `postulacion(id, licitacion_id FK, pyme_id ref, fecha_postulacion date, estado enum)`.
- `plan_suscripcion(id, nombre ESTANDAR/PREMIUM)` 1:N `suscripcion(id, usuario_id ref, plan_id FK, estado enum)` 1:N `venta(id, suscripcion_id FK, monto, fecha)` 1:1 `pago(id, venta_id FK/UNIQUE, id_transaccion, pasarela, estado enum)`.
- `notificacion(id, usuario_id ref, tipo enum, canal email)`.
- "ref" = referencia a otro microservicio, sin FK y validada por REST. Los datos técnicos adicionales van en tablas de soporte 1:1.

## 6. Endpoints
Ver `MEMORY.md` → "Endpoints implementados" y Swagger de cada servicio.

## 7. Roles
LICITADOR, PYME, ADMINISTRADOR (+ ROLE_INTERNAL para llamadas servicio a servicio).

## 8. Seguridad
BCrypt(12), JWT HS512 con expiración, RBAC en Gateway y en cada servicio, validación de ownership, Bean Validation + validador de RUT (módulo 11), JPA/Criteria con parámetros (sin SQL concatenado) + filtro SQLi en Gateway, CORS solo al frontend, Bucket4j (429), errores JSON sin stack traces, `/internal/**` con API key y nunca expuesto.

## 9. Flujo de autenticación
Registro (Pyme o Licitador) → usuario + perfil + token de confirmación (256 bits, se guarda solo su SHA-256, expira en 24 h) → correo HTML con botón → `POST /api/auth/confirmar-email` → cuenta confirmada → `POST /api/auth/login` → JWT (`sub`=userId, email, rol, perfilId, nombre, exp).

## 10. Flujo de licitaciones
BORRADOR → PUBLICADA (valida límite del plan; Premium queda destacada; correo "licitación publicada") → CERRADA (manual o automática por fecha) ↔ reabrir → ADJUDICADA (al seleccionar). CANCELADA desde cualquier estado no final. SUSPENDIDA solo por el administrador.

## 11. Flujo de postulaciones
Pyme postula (licitación abierta, una vez, dentro del límite del plan) → correo al licitador y confirmación a la pyme → EN_REVISION / RECHAZADA → seleccionar: SELECCIONADA, el resto RECHAZADA, licitación ADJUDICADA; ambas partes reciben correo (la pyme seleccionada recibe el contacto del licitador). La pyme puede editar su postulación mientras está ENVIADA, o retirarla.

## 12. Flujo de suscripciones
Sin suscripción → Estándar ACTIVA automática (gratis, 3 publicaciones, 5 postulaciones y 50 búsquedas al mes). Premium ($24.990, 30 días, ilimitado, visibilidad, soporte prioritario, LicitAsist) solo tras un pago confirmado; al vencer, vuelve a Estándar.

## 13. Flujo de Webpay (sandbox)
Venta PENDIENTE → create en Transbank (token/URL) → usuario paga → retorno al Gateway → commit server-side → AUTHORIZED + código 0 + monto + orden → APROBADO + Premium + correo con voucher/PDF. Si falla: RECHAZADO sin Premium y correo de rechazo. Si el usuario aborta: ANULADO. Idempotente, con bloqueo pesimista.

## 14. Flujo de notificaciones
Usuarios, Licitaciones y Ventas → `POST /internal/notificaciones` (después del commit, asíncrono) → plantilla Thymeleaf → SMTP → `notificacion` + `notificacion_envio` (ENVIADO/FALLIDO) → bandeja del usuario (campana).

## 15. Flujo de LicitAsist
Frontend → Gateway (20 req/min) → Asistente: verifica acceso (Admin, o Premium según Ventas) → arma contexto con el JWT del usuario (cada API aplica RBAC) y una lista blanca de campos → Groq `openai/gpt-oss-120b` → respuesta en markdown + fuentes.

## 16-17. Bases de datos y comunicación
4 bases PostgreSQL (`licitawatch_usuarios`, `_licitaciones`, `_ventas`, `_notificaciones`), migradas con Flyway. REST: Licitaciones→Usuarios/Ventas/Notificaciones, Ventas→Usuarios/Notificaciones, Usuarios→Notificaciones, Notificaciones→Usuarios, Asistente→Usuarios/Licitaciones/Ventas.

## 18. Estructura de carpetas
Ver README §20.

## 19. Plan de implementación (ejecutado)
FASE 1 Usuarios → 2 Licitaciones → 3 Ventas → 4 Notificaciones → 5 Asistente → 6 Gateway → 7 Postman + tests → 8 Frontend → 9 E2E. Checkpoint en `MEMORY.md` tras cada fase.

## 20. Estrategia de pruebas Postman
Colección ordenada que crea sus propios datos (emails alias y RUT válidos), guarda JWT e IDs automáticamente y cubre todos los endpoints, con pruebas positivas y negativas (400, 401, 403, 404, 409, 429 y 5xx). Test global sin stack traces. Pasos humanos (token del correo, pago sandbox) son opcionales y se omiten si no aplican.

## 21-23. Estado
Ver `MEMORY.md` (fuente permanente del estado del proyecto).
