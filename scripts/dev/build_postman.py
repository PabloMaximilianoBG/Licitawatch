"""Genera postman/LicitaWatch.postman_collection.json y postman/LicitaWatch.postman_environment.json.

Uso: python scripts/dev/build_postman.py
La colección está pensada para ejecutarse EN ORDEN (Collection Runner o Newman) contra el sistema real
levantado localmente: API Gateway + 5 microservicios + PostgreSQL.
"""
import json
import os
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "postman")


# --------------------------------------------------------------------------- helpers
def status(code):
    return [f'pm.test("HTTP {code}", () => pm.response.to.have.status({code}));']


def code_is(code):
    return [f'pm.test("code = {code}", () => pm.expect(pm.response.json().code).to.eql("{code}"));']


def save(var, expr):
    return [f'pm.environment.set("{var}", {expr});']


def json_eq(path, value):
    v = json.dumps(value)
    return [f'pm.test("{path} = {value}", () => pm.expect(pm.response.json(){path}).to.eql({v}));']


def req(name, method, path, auth=None, body=None, tests=None, pre=None, base="gatewayUrl",
        desc=None, no_redirect=False, headers=None):
    url_raw = "{{" + base + "}}" + path
    host_and_path = path.split("?")[0]
    query = []
    if "?" in path:
        for part in path.split("?", 1)[1].split("&"):
            k, _, v = part.partition("=")
            query.append({"key": k, "value": v})
    item = {
        "name": name,
        "event": [],
        "request": {
            "method": method,
            "header": [],
            "url": {
                "raw": url_raw,
                "host": ["{{" + base + "}}"],
                "path": [p for p in host_and_path.strip("/").split("/") if p],
            },
        },
        "response": [],
    }
    if query:
        item["request"]["url"]["query"] = query
    if desc:
        item["request"]["description"] = desc
    if auth:
        item["request"]["auth"] = {"type": "bearer", "bearer": [{"key": "token", "value": "{{" + auth + "}}", "type": "string"}]}
    else:
        item["request"]["auth"] = {"type": "noauth"}
    for k, v in (headers or {}).items():
        item["request"]["header"].append({"key": k, "value": v})
    if body is not None:
        item["request"]["header"].append({"key": "Content-Type", "value": "application/json"})
        item["request"]["body"] = {"mode": "raw", "raw": json.dumps(body, ensure_ascii=False, indent=2),
                                   "options": {"raw": {"language": "json"}}}
    if pre:
        item["event"].append({"listen": "prerequest", "script": {"type": "text/javascript", "exec": pre}})
    if tests:
        item["event"].append({"listen": "test", "script": {"type": "text/javascript", "exec": tests}})
    if no_redirect:
        item["protocolProfileBehavior"] = {"followRedirects": False}
    return item


def folder(name, items, desc=None):
    f = {"name": name, "item": items}
    if desc:
        f["description"] = desc
    return f


SKIP_SI_NO_PREMIUM = lambda jwt_var: [  # noqa: E731
    "// Solo se ejecuta si el usuario tiene Premium activo (pago Webpay sandbox completado)",
    "pm.sendRequest({url: pm.environment.get('gatewayUrl') + '/api/suscripciones/mi', method: 'GET',",
    f"  header: {{Authorization: 'Bearer ' + pm.environment.get('{jwt_var}')}}}}, (err, res) => {{",
    "  if (err || res.code !== 200 || !res.json().esPremium) {",
    "    console.log('Omitido: el usuario no tiene Premium activo. Completa el pago en Webpay sandbox primero.');",
    "    pm.execution.skipRequest();",
    "  }",
    "});",
]

LIC_BODY = {
    "titulo": "Soporte técnico TI para oficinas regionales",
    "descripcion": "Servicio de soporte técnico presencial y remoto para 3 oficinas, mesa de ayuda y mantención preventiva.",
    "requisitos": "Experiencia mínima de 3 años; técnicos certificados ITIL; boleta de garantía.",
    "condiciones": "Pago a 30 días contra entrega de informe mensual.",
    "categoria": "Tecnología",
    "region": "Metropolitana",
    "montoEstimado": 12500000,
    "fechaCierre": "{{fechaCierre}}",
    "publicar": False,
}
POST_BODY = {"propuesta": "Ofrecemos soporte con 5 técnicos certificados ITIL, SLA de 4 horas y reportes mensuales.",
             "montoOfertado": 11900000, "plazoEntregaDias": 15}

# --------------------------------------------------------------------------- 00 Setup
setup = folder("00 · Setup y salud", [
    req("Salud del Gateway", "GET", "/actuator/health",
        pre=[
            "// Datos únicos por ejecución: emails (alias +tag del buzón de pruebas) y RUT chilenos válidos",
            "const base = pm.environment.get('emailPrueba') || 'licitawatch@gmail.com';",
            "const [u, d] = base.split('@');",
            "const run = Date.now().toString().slice(-7);",
            "pm.environment.set('runId', run);",
            "const mail = t => `${u}+${t}${run}@${d}`;",
            "['pyme','pyme2','licitador','licitador2','regpyme','reglicitador','temporal'].forEach(t => pm.environment.set(t + 'Mail', mail(t)));",
            "pm.environment.set('pymeEmail', mail('pyme'));",
            "pm.environment.set('licitadorEmail', mail('licitador'));",
            "function rut() {",
            "  const body = String(Math.floor(10000000 + Math.random() * 89999999));",
            "  let s = 0, m = 2;",
            "  for (let i = body.length - 1; i >= 0; i--) { s += parseInt(body[i]) * m; m = m === 7 ? 2 : m + 1; }",
            "  const r = 11 - (s % 11);",
            "  return body + '-' + (r === 11 ? '0' : r === 10 ? 'K' : String(r));",
            "}",
            "['pymeRut','pyme2Rut','licitadorRut','licitador2Rut','regPymeRut','regLicitadorRut','tempRut'].forEach(k => pm.environment.set(k, rut()));",
            "const cierre = new Date(Date.now() + 20 * 86400000).toISOString().slice(0, 10);",
            "pm.environment.set('fechaCierre', cierre);",
        ],
        tests=status(200) + ['pm.test("Gateway UP", () => pm.expect(pm.response.json().status).to.eql("UP"));']),
    req("Salud API Usuarios (directo)", "GET", "/actuator/health", base="usersUrl", tests=status(200)),
    req("Salud API Licitaciones (directo)", "GET", "/actuator/health", base="licitacionesUrl", tests=status(200)),
    req("Salud API Ventas (directo)", "GET", "/actuator/health", base="ventasUrl", tests=status(200)),
    req("Salud API Notificaciones (directo)", "GET", "/actuator/health", base="notificacionesUrl", tests=status(200)),
    req("Salud API Asistente (directo)", "GET", "/actuator/health", base="asistenteUrl", tests=status(200)),
])

# --------------------------------------------------------------------------- 01 Auth
auth = folder("01 · Auth (registro, confirmación, login)", [
    req("Login Administrador", "POST", "/api/auth/login",
        body={"email": "{{adminEmail}}", "password": "{{adminPassword}}"},
        tests=status(200) + [
            'const r = pm.response.json();',
            'pm.test("Devuelve JWT Bearer", () => { pm.expect(r.token).to.be.a("string"); pm.expect(r.tipo).to.eql("Bearer"); });',
            'pm.test("Rol ADMINISTRADOR", () => pm.expect(r.usuario.rol).to.eql("ADMINISTRADOR"));',
            'pm.test("No expone contraseña", () => pm.expect(JSON.stringify(r)).to.not.include("password"));',
            'pm.environment.set("adminJwt", r.token); pm.environment.set("jwt", r.token);',
        ]),
    req("Registro Pyme", "POST", "/api/auth/registro/pyme",
        body={"email": "{{regpymeMail}}", "password": "Pyme12345", "confirmPassword": "Pyme12345",
              "rut": "{{regPymeRut}}", "nombreContacto": "Pyme Registro Postman"},
        tests=status(201) + json_eq(".rol", "PYME") + save("userId", "pm.response.json().usuarioId") + [
            'pm.test("Informa envío de correo de confirmación", () => pm.expect(pm.response.json()).to.have.property("correoEnviado"));']),
    req("Registro Licitador", "POST", "/api/auth/registro/licitador",
        body={"email": "{{reglicitadorMail}}", "password": "Licita12345", "confirmPassword": "Licita12345",
              "rut": "{{regLicitadorRut}}", "razonSocial": "Constructora Registro Postman SpA"},
        tests=status(201) + json_eq(".rol", "LICITADOR")),
    req("409 · Registro con email duplicado", "POST", "/api/auth/registro/pyme",
        body={"email": "{{regpymeMail}}", "password": "Pyme12345", "confirmPassword": "Pyme12345",
              "rut": "{{tempRut}}", "nombreContacto": "Duplicado"},
        tests=status(409) + code_is("EMAIL_DUPLICADO")),
    req("409 · Registro con RUT duplicado", "POST", "/api/auth/registro/pyme",
        body={"email": "otro.{{runId}}@licitawatch.test", "password": "Pyme12345", "confirmPassword": "Pyme12345",
              "rut": "{{regPymeRut}}", "nombreContacto": "RUT duplicado"},
        tests=status(409) + code_is("RUT_DUPLICADO")),
    req("400 · Registro con campos faltantes", "POST", "/api/auth/registro/licitador", body={"email": "x@licitawatch.test"},
        tests=status(400) + code_is("VALIDACION") + [
            'pm.test("Detalla campos", () => { const e = pm.response.json().errores; pm.expect(e).to.have.property("password"); pm.expect(e).to.have.property("rut"); pm.expect(e).to.have.property("razonSocial"); });']),
    req("400 · Registro con datos inválidos (email, RUT, contraseña débil)", "POST", "/api/auth/registro/pyme",
        body={"email": "no-es-email", "password": "abc", "confirmPassword": "abc", "rut": "12345678-9", "nombreContacto": "X"},
        tests=status(400) + [
            'const e = pm.response.json().errores;',
            'pm.test("email, password, rut y nombreContacto inválidos", () => ["email","password","rut","nombreContacto"].forEach(k => pm.expect(e).to.have.property(k)));']),
    req("400 · Contraseña y confirmación no coinciden", "POST", "/api/auth/registro/pyme",
        body={"email": "nocoincide.{{runId}}@licitawatch.test", "password": "Pyme12345", "confirmPassword": "Pyme99999",
              "rut": "{{tempRut}}", "nombreContacto": "Pyme"},
        tests=status(400) + code_is("PASSWORD_NO_COINCIDE")),
    req("403 · Login sin confirmar correo", "POST", "/api/auth/login",
        body={"email": "{{regpymeMail}}", "password": "Pyme12345"}, tests=status(403) + code_is("EMAIL_NO_CONFIRMADO")),
    req("400 · Confirmar email con token inválido", "POST", "/api/auth/confirmar-email", body={"token": "token-invalido"},
        tests=status(400) + code_is("TOKEN_INVALIDO")),
    req("Confirmar email (token recibido por correo)", "POST", "/api/auth/confirmar-email", body={"token": "{{confirmToken}}"},
        desc="Copia el token del enlace del correo de confirmación (…/confirm-email?token=XXXX) en la variable de entorno `confirmToken`. Si está vacía, la request se omite.",
        pre=["if (!pm.environment.get('confirmToken')) { console.log('Omitido: define confirmToken con el token del correo'); pm.execution.skipRequest(); }"],
        tests=status(200)),
    req("Reenviar correo de confirmación", "POST", "/api/auth/reenviar-confirmacion", body={"email": "{{regpymeMail}}"},
        tests=status(200)),
    req("401 · Login con credenciales inválidas", "POST", "/api/auth/login",
        body={"email": "{{adminEmail}}", "password": "Incorrecta123"}, tests=status(401) + code_is("CREDENCIALES_INVALIDAS")),
])

# --------------------------------------------------------------------------- 02 Usuarios
def crear_usuario(nombre, rol, mail, rut_var, extra, pwd, vars_save):
    body = {"email": "{{" + mail + "}}", "password": pwd, "rol": rol, "rut": "{{" + rut_var + "}}"}
    body.update(extra)
    tests = status(201) + json_eq(".rol", rol) + json_eq(".emailConfirmado", True)
    for var, expr in vars_save:
        tests += save(var, expr)
    return req(nombre, "POST", "/api/admin/usuarios", auth="adminJwt", body=body, tests=tests)


def login(nombre, mail_var, pwd, jwt_var, extra_save=()):
    t = status(200) + save(jwt_var, "pm.response.json().token")
    for var, expr in extra_save:
        t += save(var, expr)
    return req(nombre, "POST", "/api/auth/login", body={"email": "{{" + mail_var + "}}", "password": pwd}, tests=t)


usuarios = folder("02 · Usuarios y administración de usuarios", [
    crear_usuario("Admin crea Pyme (confirmada)", "PYME", "pymeEmail", "pymeRut", {"nombreContacto": "Carla Pyme Postman"},
                  "Pyme12345", [("pymeUserId", "pm.response.json().id"), ("pymeId", "pm.response.json().perfilId")]),
    crear_usuario("Admin crea Pyme 2", "PYME", "pyme2Mail", "pyme2Rut", {"nombreContacto": "Pedro Pyme Dos"},
                  "Pyme12345", [("pyme2UserId", "pm.response.json().id")]),
    crear_usuario("Admin crea Licitador (confirmado)", "LICITADOR", "licitadorEmail", "licitadorRut",
                  {"razonSocial": "Constructora Andes Postman SpA"}, "Licita12345",
                  [("licitadorUserId", "pm.response.json().id"), ("licitadorId", "pm.response.json().perfilId")]),
    crear_usuario("Admin crea Licitador 2", "LICITADOR", "licitador2Mail", "licitador2Rut",
                  {"razonSocial": "Otra Empresa Licitadora SpA"}, "Licita12345", [("licitador2UserId", "pm.response.json().id")]),
    login("Login Pyme", "pymeEmail", "Pyme12345", "pymeJwt"),
    login("Login Pyme 2", "pyme2Mail", "Pyme12345", "pyme2Jwt"),
    login("Login Licitador", "licitadorEmail", "Licita12345", "licitadorJwt"),
    login("Login Licitador 2", "licitador2Mail", "Licita12345", "licitador2Jwt"),
    req("Consultar mi perfil (Pyme)", "GET", "/api/usuarios/me", auth="pymeJwt",
        tests=status(200) + json_eq(".rol", "PYME") + [
            'pm.test("RUT y nombre de contacto", () => { pm.expect(pm.response.json().rut).to.eql(pm.environment.get("pymeRut")); });']),
    req("Actualizar mi perfil (Pyme)", "PUT", "/api/usuarios/me", auth="pymeJwt", body={"nombreContacto": "Carla Pyme Editada"},
        tests=status(200) + json_eq(".nombreContacto", "Carla Pyme Editada")),
    req("Actualizar mi perfil (Licitador)", "PUT", "/api/usuarios/me", auth="licitadorJwt", body={"razonSocial": "Constructora Andes Postman SpA"},
        tests=status(200)),
    req("400 · Cambiar contraseña con contraseña actual incorrecta", "PUT", "/api/usuarios/me/password", auth="pymeJwt",
        body={"passwordActual": "Incorrecta1", "passwordNueva": "Nueva12345", "confirmPassword": "Nueva12345"},
        tests=status(400) + code_is("PASSWORD_ACTUAL_INCORRECTA")),
    req("Admin · Listar usuarios (filtro rol=PYME)", "GET", "/api/admin/usuarios?rol=PYME&page=0&size=20", auth="adminJwt",
        tests=status(200) + ['pm.test("Paginado", () => pm.expect(pm.response.json()).to.have.property("totalElements"));']),
    req("Admin · Buscar usuarios (q)", "GET", "/api/admin/usuarios?q=postman", auth="adminJwt",
        tests=status(200) + ['pm.test("Encuentra usuarios", () => pm.expect(pm.response.json().totalElements).to.be.above(0));']),
    req("Admin · Obtener usuario por ID", "GET", "/api/admin/usuarios/{{pymeUserId}}", auth="adminJwt",
        tests=status(200) + ['pm.test("ID correcto", () => pm.expect(pm.response.json().id).to.eql(Number(pm.environment.get("pymeUserId"))));']),
    req("Admin · Editar usuario", "PUT", "/api/admin/usuarios/{{pyme2UserId}}", auth="adminJwt",
        body={"nombreContacto": "Pedro Pyme Editado"}, tests=status(200) + json_eq(".nombreContacto", "Pedro Pyme Editado")),
    crear_usuario("Admin · Crear usuario temporal", "PYME", "temporalMail", "tempRut", {"nombreContacto": "Usuario Temporal"},
                  "Temp12345", [("tempUserId", "pm.response.json().id")]),
    req("Admin · Desactivar usuario", "PATCH", "/api/admin/usuarios/{{tempUserId}}/estado", auth="adminJwt", body={"activo": False},
        tests=status(200) + json_eq(".activo", False)),
    req("403 · Login de usuario desactivado", "POST", "/api/auth/login", body={"email": "{{temporalMail}}", "password": "Temp12345"},
        tests=status(403) + code_is("CUENTA_INACTIVA")),
    req("Admin · Activar usuario", "PATCH", "/api/admin/usuarios/{{tempUserId}}/estado", auth="adminJwt", body={"activo": True},
        tests=status(200) + json_eq(".activo", True)),
    req("Admin · Cambiar rol (PYME → LICITADOR)", "PATCH", "/api/admin/usuarios/{{tempUserId}}/rol", auth="adminJwt",
        body={"rol": "LICITADOR", "rut": "{{tempRut}}", "razonSocial": "Temporal Convertido SpA"},
        tests=status(200) + json_eq(".rol", "LICITADOR")),
    req("Admin · Listar roles", "GET", "/api/admin/roles", auth="adminJwt",
        tests=status(200) + ['pm.test("3 roles", () => pm.expect(pm.response.json().map(r => r.nombre)).to.have.members(["LICITADOR","PYME","ADMINISTRADOR"]));']),
    req("Admin · Estadísticas de usuarios", "GET", "/api/admin/usuarios/estadisticas", auth="adminJwt",
        tests=status(200) + ['pm.test("Tiene conteo por rol", () => pm.expect(pm.response.json().porRol).to.have.property("PYME"));']),
    req("400 · Admin intenta desactivarse a sí mismo", "PATCH", "/api/admin/usuarios/1/estado", auth="adminJwt", body={"activo": False},
        pre=["// El admin inicial suele ser id 1; se valida con su propio id desde /me",
             "pm.sendRequest({url: pm.environment.get('gatewayUrl') + '/api/usuarios/me', header: {Authorization: 'Bearer ' + pm.environment.get('adminJwt')}}, (e, r) => {",
             "  if (!e && r.code === 200) { pm.request.url.path[3] = String(r.json().id); }",
             "});"],
        tests=status(400) + code_is("AUTO_DESACTIVACION")),
    req("404 · Admin obtiene usuario inexistente", "GET", "/api/admin/usuarios/99999999", auth="adminJwt", tests=status(404)),
    req("409 · Admin crea usuario con email existente", "POST", "/api/admin/usuarios", auth="adminJwt",
        body={"email": "{{pymeEmail}}", "password": "Pyme12345", "rol": "PYME", "rut": "{{tempRut}}", "nombreContacto": "Email Duplicado"},
        tests=status(409) + code_is("EMAIL_DUPLICADO")),
])

# --------------------------------------------------------------------------- 03 Licitaciones
def lic_body(**kw):
    b = dict(LIC_BODY)
    b.update(kw)
    return b


licitaciones = folder("03 · Licitaciones (Licitador y Pyme)", [
    req("Crear licitación (borrador)", "POST", "/api/licitaciones", auth="licitadorJwt", body=lic_body(),
        tests=status(201) + json_eq(".estado", "BORRADOR") + save("licitacionId", "pm.response.json().id") + [
            'pm.test("licitadorId = mi perfil", () => pm.expect(pm.response.json().licitadorId).to.eql(Number(pm.environment.get("licitadorId"))));']),
    req("Actualizar licitación", "PUT", "/api/licitaciones/{{licitacionId}}", auth="licitadorJwt",
        body=lic_body(titulo="Soporte técnico TI para oficinas regionales (actualizada)", montoEstimado=13000000),
        tests=status(200) + json_eq(".montoEstimado", 13000000)),
    req("Cambiar estado → PUBLICADA", "PATCH", "/api/licitaciones/{{licitacionId}}/estado", auth="licitadorJwt",
        body={"estado": "PUBLICADA"}, tests=status(200) + json_eq(".estado", "PUBLICADA")),
    req("Crear y publicar licitación directamente", "POST", "/api/licitaciones", auth="licitadorJwt",
        body=lic_body(titulo="Auditoría de ciberseguridad", categoria="Tecnología", region="Valparaíso",
                      montoEstimado=8000000, publicar=True),
        tests=status(201) + json_eq(".estado", "PUBLICADA") + save("licitacion2Id", "pm.response.json().id")),
    req("Crear borrador para eliminar", "POST", "/api/licitaciones", auth="licitadorJwt",
        body=lic_body(titulo="Borrador temporal a eliminar"),
        tests=status(201) + save("licitacionBorradorId", "pm.response.json().id")),
    req("Eliminar licitación propia (sin postulaciones)", "DELETE", "/api/licitaciones/{{licitacionBorradorId}}", auth="licitadorJwt",
        tests=status(204)),
    req("404 · Obtener licitación eliminada", "GET", "/api/licitaciones/{{licitacionBorradorId}}", auth="licitadorJwt", tests=status(404)),
    req("Listar licitaciones publicadas (Pyme)", "GET", "/api/licitaciones?page=0&size=12", auth="pymeJwt",
        tests=status(200) + ['pm.test("Solo PUBLICADAS", () => pm.response.json().content.forEach(l => pm.expect(l.estado).to.eql("PUBLICADA")));']),
    req("Buscar licitaciones (q)", "GET", "/api/licitaciones?q=soporte", auth="pymeJwt",
        tests=status(200) + ['pm.test("Encuentra la licitación", () => pm.expect(pm.response.json().content.map(l => l.id)).to.include(Number(pm.environment.get("licitacionId"))));']),
    req("Filtrar licitaciones (categoría, región, monto, cierre)", "GET",
        "/api/licitaciones?categoria=Tecnología&region=Valparaíso&montoMin=1000000&montoMax=9000000&cierreHasta={{fechaCierre}}&orden=monto_desc",
        auth="pymeJwt",
        tests=status(200) + ['pm.test("Filtros aplicados", () => pm.response.json().content.forEach(l => { pm.expect(l.region).to.eql("Valparaíso"); pm.expect(l.montoEstimado).to.be.at.most(9000000); }));']),
    req("Obtener licitación por ID (Pyme ve requisitos)", "GET", "/api/licitaciones/{{licitacionId}}", auth="pymeJwt",
        tests=status(200) + ['pm.test("Tiene requisitos y condiciones", () => { const r = pm.response.json(); pm.expect(r.requisitos).to.be.a("string"); pm.expect(r.condiciones).to.be.a("string"); });']),
    req("Mis licitaciones (Licitador)", "GET", "/api/licitaciones/mias", auth="licitadorJwt",
        tests=status(200) + ['pm.test("Incluye mis licitaciones", () => pm.expect(pm.response.json().length).to.be.at.least(2));']),
    req("Resumen dashboard (Licitador)", "GET", "/api/licitaciones/resumen", auth="licitadorJwt",
        tests=status(200) + ['pm.test("KPIs", () => pm.expect(pm.response.json().kpis).to.have.property("licitacionesCreadas"));']),
    req("Uso del plan (Licitador)", "GET", "/api/licitaciones/uso-plan", auth="licitadorJwt",
        tests=status(200) + ['pm.test("Plan ESTANDAR con límite", () => pm.expect(pm.response.json().limitePublicacionesMes).to.eql(3));']),
    req("403 · Pyme intenta crear licitación (rol incorrecto)", "POST", "/api/licitaciones", auth="pymeJwt", body=lic_body(),
        tests=status(403)),
    req("403 · Licitador modifica licitación ajena (ownership)", "PUT", "/api/licitaciones/{{licitacionId}}", auth="licitador2Jwt",
        body=lic_body(titulo="Intento de modificar licitación ajena"), tests=status(403) + code_is("NO_ES_DUENO")),
    req("403 · Licitador elimina licitación ajena", "DELETE", "/api/licitaciones/{{licitacionId}}", auth="licitador2Jwt", tests=status(403)),
    req("400 · Crear licitación con datos inválidos", "POST", "/api/licitaciones", auth="licitadorJwt",
        body={"titulo": "x", "montoEstimado": -1, "fechaCierre": "2020-01-01"},
        tests=status(400) + ['pm.test("Errores por campo", () => { const e = pm.response.json().errores; ["titulo","montoEstimado","fechaCierre","descripcion","categoria"].forEach(k => pm.expect(e).to.have.property(k)); });']),
    req("400 · Estado ADJUDICADA no se asigna manualmente", "PATCH", "/api/licitaciones/{{licitacionId}}/estado", auth="licitadorJwt",
        body={"estado": "ADJUDICADA"}, tests=status(400) + code_is("TRANSICION_INVALIDA")),
    req("404 · Licitación inexistente", "GET", "/api/licitaciones/99999999", auth="pymeJwt", tests=status(404)),
])

# --------------------------------------------------------------------------- 04 Postulaciones
postulaciones = folder("04 · Postulaciones", [
    req("Crear postulación (Pyme)", "POST", "/api/licitaciones/{{licitacionId}}/postulaciones", auth="pymeJwt", body=POST_BODY,
        tests=status(201) + json_eq(".estado", "ENVIADA") + save("postulacionId", "pm.response.json().id")),
    req("409 · Postulación duplicada", "POST", "/api/licitaciones/{{licitacionId}}/postulaciones", auth="pymeJwt", body=POST_BODY,
        tests=status(409) + code_is("POSTULACION_DUPLICADA")),
    req("Crear postulación (Pyme 2)", "POST", "/api/licitaciones/{{licitacionId}}/postulaciones", auth="pyme2Jwt",
        body={**POST_BODY, "montoOfertado": 12400000, "plazoEntregaDias": 20},
        tests=status(201) + save("postulacion2Id", "pm.response.json().id")),
    req("403 · Licitador intenta postular (rol incorrecto)", "POST", "/api/licitaciones/{{licitacion2Id}}/postulaciones",
        auth="licitadorJwt", body=POST_BODY, tests=status(403)),
    req("400 · Postulación con datos inválidos", "POST", "/api/licitaciones/{{licitacion2Id}}/postulaciones", auth="pymeJwt",
        body={"propuesta": "corta", "montoOfertado": 0}, tests=status(400)),
    req("Mis postulaciones (Pyme)", "GET", "/api/postulaciones/mias", auth="pymeJwt",
        tests=status(200) + ['pm.test("Incluye la postulación", () => pm.expect(pm.response.json().map(p => p.id)).to.include(Number(pm.environment.get("postulacionId"))));']),
    req("Consultar postulación", "GET", "/api/postulaciones/{{postulacionId}}", auth="pymeJwt",
        tests=status(200) + json_eq(".estado", "ENVIADA")),
    req("Modificar postulación (Pyme, estado ENVIADA)", "PUT", "/api/postulaciones/{{postulacionId}}", auth="pymeJwt",
        body={**POST_BODY, "montoOfertado": 11500000}, tests=status(200) + json_eq(".montoOfertado", 11500000)),
    req("403 · Pyme consulta postulación ajena", "GET", "/api/postulaciones/{{postulacion2Id}}", auth="pymeJwt",
        tests=status(403) + code_is("NO_ES_DUENO")),
    req("Revisar postulantes (Licitador dueño)", "GET", "/api/licitaciones/{{licitacionId}}/postulaciones", auth="licitadorJwt",
        tests=status(200) + ['pm.test("2 postulantes con datos de la pyme", () => { const l = pm.response.json(); pm.expect(l.length).to.eql(2); pm.expect(l[0].pymeNombre).to.be.a("string"); });']),
    req("403 · Otro licitador revisa postulantes", "GET", "/api/licitaciones/{{licitacionId}}/postulaciones", auth="licitador2Jwt",
        tests=status(403)),
    req("Cambiar estado de postulación → EN_REVISION", "PATCH", "/api/postulaciones/{{postulacionId}}/estado", auth="licitadorJwt",
        body={"estado": "EN_REVISION", "comentario": "Revisando antecedentes técnicos"}, tests=status(200) + json_eq(".estado", "EN_REVISION")),
    req("403 · Otro licitador selecciona postulante", "POST", "/api/postulaciones/{{postulacionId}}/seleccionar", auth="licitador2Jwt",
        body={"comentario": "x"}, tests=status(403)),
    req("Seleccionar postulante (adjudica y notifica)", "POST", "/api/postulaciones/{{postulacionId}}/seleccionar", auth="licitadorJwt",
        body={"comentario": "Mejor relación precio/plazo y equipo certificado"}, tests=status(200) + json_eq(".estado", "SELECCIONADA")),
    req("Licitación queda ADJUDICADA", "GET", "/api/licitaciones/{{licitacionId}}", auth="licitadorJwt",
        tests=status(200) + json_eq(".estado", "ADJUDICADA")),
    req("Postulación no seleccionada queda RECHAZADA", "GET", "/api/postulaciones/{{postulacion2Id}}", auth="pyme2Jwt",
        tests=status(200) + json_eq(".estado", "RECHAZADA")),
    req("Pyme seleccionada recibe contacto del licitador", "GET", "/api/postulaciones/mias", auth="pymeJwt",
        tests=status(200) + ['pm.test("licitadorEmail visible solo al seleccionado", () => { const p = pm.response.json().find(x => x.id == pm.environment.get("postulacionId")); pm.expect(p.licitadorEmail).to.be.a("string"); });']),
    req("Postular a otra licitación (para retirar)", "POST", "/api/licitaciones/{{licitacion2Id}}/postulaciones", auth="pyme2Jwt", body=POST_BODY,
        tests=status(201) + save("postulacionRetiroId", "pm.response.json().id")),
    req("Retirar (cancelar) postulación", "PATCH", "/api/postulaciones/{{postulacionRetiroId}}/retirar", auth="pyme2Jwt",
        tests=status(200) + json_eq(".estado", "RETIRADA")),
    req("409 · Eliminar licitación con postulaciones", "DELETE", "/api/licitaciones/{{licitacionId}}", auth="licitadorJwt",
        tests=status(409) + code_is("TIENE_POSTULACIONES")),
])

# --------------------------------------------------------------------------- 05 Planes
planes = folder("05 · Planes", [
    req("Listar planes (público)", "GET", "/api/planes",
        tests=status(200) + ['pm.test("Estándar y Premium", () => pm.expect(pm.response.json().map(p => p.nombre)).to.have.members(["ESTANDAR","PREMIUM"]));']),
    req("Consultar plan Premium", "GET", "/api/planes/nombre/PREMIUM",
        tests=status(200) + json_eq(".precioFormateado", "$24.990") + json_eq(".accesoLicitasist", True) + save("planId", "pm.response.json().id")),
    req("Consultar plan Estándar", "GET", "/api/planes/nombre/ESTANDAR",
        tests=status(200) + json_eq(".accesoLicitasist", False) + save("planEstandarId", "pm.response.json().id")),
    req("Consultar plan por ID", "GET", "/api/planes/{{planId}}", tests=status(200) + json_eq(".nombre", "PREMIUM")),
    req("404 · Plan inexistente", "GET", "/api/planes/9999", tests=status(404)),
])

# --------------------------------------------------------------------------- 06 Ventas / Webpay
ventas = folder("06 · Suscripciones, ventas y Webpay Plus (sandbox)", [
    req("Consultar mi suscripción (Estándar automático)", "GET", "/api/suscripciones/mi", auth="pymeJwt",
        tests=status(200) + ['pm.test("Plan ESTANDAR sin LicitAsist", () => { const s = pm.response.json(); pm.expect(s.plan.nombre).to.eql("ESTANDAR"); pm.expect(s.accesoLicitasist).to.eql(false); });']
        + save("suscripcionId", "pm.response.json().id")),
    req("400 · Comprar plan Estándar (gratuito)", "POST", "/api/ventas", auth="pymeJwt", body={"planId": "{{planEstandarId}}"},
        tests=status(400) + code_is("PLAN_GRATUITO")),
    req("403 · Administrador no compra planes", "POST", "/api/ventas", auth="adminJwt", body={"planId": "{{planId}}"},
        tests=status(403)),
    req("Crear venta Premium (pendiente)", "POST", "/api/ventas", auth="pymeJwt", body={"planId": "{{planId}}"},
        tests=status(201) + json_eq(".estadoPago", "PENDIENTE") + json_eq(".montoFormateado", "$24.990")
        + save("ventaId", "pm.response.json().id") + save("pagoId", "pm.response.json().pagoId") + save("buyOrder", "pm.response.json().numeroOrden")),
    req("Consultar venta", "GET", "/api/ventas/{{ventaId}}", auth="pymeJwt", tests=status(200) + json_eq(".plan", "PREMIUM")),
    req("403 · Otro usuario consulta la venta", "GET", "/api/ventas/{{ventaId}}", auth="pyme2Jwt", tests=status(403)),
    req("Crear transacción Webpay (sandbox Transbank)", "POST", "/api/pagos/webpay/transacciones", auth="pymeJwt",
        body={"ventaId": "{{ventaId}}"},
        tests=status(200) + [
            'const r = pm.response.json();',
            'pm.test("Token y URL de Webpay integración", () => { pm.expect(r.token).to.have.lengthOf(64); pm.expect(r.url).to.include("webpay3gint.transbank.cl"); });',
            'pm.environment.set("webpayToken", r.token); pm.environment.set("webpayUrl", r.url);',
            'console.log("Para pagar en el sandbox abre: postman/pagar-webpay.html?token=" + r.token);',
        ]),
    req("409 · Confirmar pago aún no completado en Webpay", "POST", "/api/pagos/webpay/confirmar", auth="pymeJwt",
        body={"token": "{{webpayToken}}"},
        pre=["if (pm.environment.get('pagoCompletado') === 'si') { pm.execution.skipRequest(); }"],
        tests=status(409) + code_is("PAGO_NO_COMPLETADO")),
    req("[MANUAL] Confirmar pago tras pagar en Webpay sandbox", "POST", "/api/pagos/webpay/confirmar", auth="pymeJwt",
        body={"token": "{{webpayToken}}"},
        desc="1) Abre postman/pagar-webpay.html?token={{webpayToken}} en el navegador. 2) Paga con la tarjeta de prueba VISA 4051 8856 0044 6623, CVV 123, RUT 11.111.111-1, clave 123 (aprobada) o Mastercard 5186 0595 5959 0568 (rechazada). 3) Webpay vuelve al Gateway, que confirma automáticamente. 4) Define pagoCompletado=si y ejecuta esta request (idempotente).",
        pre=["if (pm.environment.get('pagoCompletado') !== 'si') { console.log('Omitido: completa el pago en Webpay sandbox y define pagoCompletado=si'); pm.execution.skipRequest(); }"],
        tests=status(200) + ['pm.test("Pago confirmado por el backend", () => pm.expect(["APROBADO","RECHAZADO"]).to.include(pm.response.json().estado));']),
    req("Consultar pago", "GET", "/api/pagos/{{pagoId}}", auth="pymeJwt",
        tests=status(200) + ['pm.test("Pasarela Webpay Plus", () => pm.expect(pm.response.json().pasarela).to.eql("WEBPAY_PLUS"));']),
    req("Consultar pago por número de orden", "GET", "/api/pagos/orden/{{buyOrder}}", auth="pymeJwt",
        tests=status(200) + ['pm.test("Misma orden", () => pm.expect(pm.response.json().numeroOrden).to.eql(pm.environment.get("buyOrder")));']),
    req("Historial de pagos", "GET", "/api/pagos/historial", auth="pymeJwt",
        tests=status(200) + ['pm.test("Contiene la orden con los campos del historial", () => { const p = pm.response.json().find(x => x.numeroOrden === pm.environment.get("buyOrder")); ["fecha","plan","monto","estado","idTransaccion","numeroOrden"].forEach(k => pm.expect(p).to.have.property(k)); });']),
    req("Comprobante (409 si no está aprobado / 200 si se pagó)", "GET", "/api/pagos/{{pagoId}}/comprobante", auth="pymeJwt",
        tests=['pm.test("200 con voucher o 409 COMPROBANTE_NO_DISPONIBLE", () => {',
               '  if (pm.response.code === 200) { const v = pm.response.json(); pm.expect(v.estado).to.eql("PAGADO / APROBADO"); pm.expect(v.pasarela).to.eql("Webpay Plus"); pm.expect(v.montoFormateado).to.eql("$24.990"); }',
               '  else { pm.expect(pm.response.code).to.eql(409); pm.expect(pm.response.json().code).to.eql("COMPROBANTE_NO_DISPONIBLE"); }',
               '});']),
    req("Comprobante PDF (solo pago aprobado)", "GET", "/api/pagos/{{pagoId}}/comprobante/pdf", auth="pymeJwt",
        pre=["if (pm.environment.get('pagoCompletado') !== 'si') { pm.execution.skipRequest(); }"],
        tests=['pm.test("PDF o 409", () => { if (pm.response.code === 200) pm.expect(pm.response.headers.get("Content-Type")).to.include("application/pdf"); else pm.expect(pm.response.code).to.eql(409); });']),
    req("Crear venta 2 (para probar pago anulado)", "POST", "/api/ventas", auth="pyme2Jwt", body={"planId": "{{planId}}"},
        tests=status(201) + save("venta2Id", "pm.response.json().id") + save("buyOrder2", "pm.response.json().numeroOrden")),
    req("Crear transacción Webpay 2", "POST", "/api/pagos/webpay/transacciones", auth="pyme2Jwt", body={"ventaId": "{{venta2Id}}"},
        tests=status(200) + save("webpayToken2", "pm.response.json().token")),
    req("Retorno Webpay: usuario anula el pago (TBK_TOKEN)", "GET",
        "/api/pagos/webpay/retorno?TBK_TOKEN={{webpayToken2}}&TBK_ORDEN_COMPRA={{buyOrder2}}&TBK_ID_SESION=postman",
        no_redirect=True,
        tests=status(302) + ['pm.test("Redirige al frontend con estado ANULADO", () => pm.expect(pm.response.headers.get("Location")).to.include("/pago/resultado?orden=").and.include("estado=ANULADO"));']),
    req("Pago anulado: no activa Premium", "GET", "/api/pagos/orden/{{buyOrder2}}", auth="pyme2Jwt",
        tests=status(200) + json_eq(".estado", "ANULADO") + json_eq(".estadoSuscripcion", "CANCELADA")),
    req("Suscripción sigue en Estándar tras pago anulado", "GET", "/api/suscripciones/mi", auth="pyme2Jwt",
        tests=status(200) + ['pm.test("Sigue ESTANDAR", () => pm.expect(pm.response.json().plan.nombre).to.eql("ESTANDAR"));']),
    req("404 · Confirmar token inexistente", "POST", "/api/pagos/webpay/confirmar", auth="pymeJwt", body={"token": "no-existe"},
        tests=status(404)),
    req("Historial de suscripciones", "GET", "/api/suscripciones/mis", auth="pymeJwt", tests=status(200)),
    req("Mis ventas", "GET", "/api/ventas/mias", auth="pymeJwt", tests=status(200)),
])

# --------------------------------------------------------------------------- 07 Notificaciones
notificaciones = folder("07 · Notificaciones (correo SMTP real)", [
    req("Admin crea notificación y envía email", "POST", "/api/notificaciones", auth="adminJwt",
        body={"usuarioId": "{{pymeUserId}}", "asunto": "Aviso de prueba LicitaWatch", "mensaje": "Este es un correo de prueba enviado desde la colección Postman."},
        tests=status(201) + json_eq(".tipo", "GENERAL") + json_eq(".canal", "EMAIL") + save("notificacionId", "pm.response.json().id") + [
            'pm.test("Envío registrado (ENVIADO o FALLIDO si SMTP no está configurado)", () => pm.expect(["ENVIADO","FALLIDO"]).to.include(pm.response.json().estado));']),
    req("Confirmar envío (estado de la notificación)", "GET", "/api/notificaciones/{{notificacionId}}", auth="pymeJwt",
        tests=status(200) + ['pm.test("Tiene estado de envío", () => pm.expect(pm.response.json().estado).to.be.oneOf(["ENVIADO","FALLIDO"]));']),
    req("Historial de mis notificaciones", "GET", "/api/notificaciones/mias", auth="pymeJwt",
        tests=status(200) + ['pm.test("Hay notificaciones (admin + postulación + resultado)", () => pm.expect(pm.response.json().totalElements).to.be.at.least(1));']),
    req("Cantidad de no leídas", "GET", "/api/notificaciones/mias/no-leidas", auth="pymeJwt",
        tests=status(200) + ['pm.test("noLeidas numérico", () => pm.expect(pm.response.json().noLeidas).to.be.a("number"));']),
    req("Marcar notificación como leída", "PATCH", "/api/notificaciones/{{notificacionId}}/leida", auth="pymeJwt",
        tests=status(200) + json_eq(".leida", True)),
    req("Marcar todas como leídas", "PATCH", "/api/notificaciones/mias/leidas", auth="pymeJwt", tests=status(200)),
    req("Historial de notificaciones del Licitador (eventos)", "GET", "/api/notificaciones/mias", auth="licitadorJwt",
        tests=status(200) + ['pm.test("Recibió eventos de licitación publicada / postulación / adjudicación", () => { const tipos = pm.response.json().content.map(n => n.tipo); pm.expect(tipos).to.include.oneOf(["LICITACION_PUBLICADA","POSTULACION_RECIBIDA","LICITACION_ADJUDICADA"]); });']),
    req("403 · Otro usuario consulta la notificación", "GET", "/api/notificaciones/{{notificacionId}}", auth="pyme2Jwt", tests=status(403)),
    req("403 · Pyme intenta crear notificación", "POST", "/api/notificaciones", auth="pymeJwt",
        body={"usuarioId": "{{pymeUserId}}", "asunto": "x", "mensaje": "y"}, tests=status(403)),
    req("Admin · Historial global de notificaciones", "GET", "/api/admin/notificaciones?page=0&size=20", auth="adminJwt", tests=status(200)),
    req("Admin · Estadísticas de notificaciones", "GET", "/api/admin/notificaciones/estadisticas", auth="adminJwt",
        tests=status(200) + ['pm.test("Por tipo y estado", () => { pm.expect(pm.response.json()).to.have.property("porTipo"); pm.expect(pm.response.json()).to.have.property("porEstado"); });']),
])

# --------------------------------------------------------------------------- 08 LicitAsist
chat = lambda msg, lid=None: ({"mensaje": msg, "historial": []} | ({"licitacionId": lid} if lid else {}))  # noqa: E731
licitasist = folder("08 · LicitAsist (Groq · openai/gpt-oss-120b)", [
    req("Estado LicitAsist · Pyme Estándar (sin acceso)", "GET", "/api/asistente/estado", auth="pymeJwt",
        tests=status(200) + ['pm.test("acceso=false (Premium requerido)", () => pm.expect(pm.response.json().acceso).to.eql(false));']),
    req("403 · Pyme sin Premium usa LicitAsist", "POST", "/api/asistente/chat", auth="pyme2Jwt", body=chat("¿Qué licitaciones están abiertas?"),
        tests=status(403) + code_is("PREMIUM_REQUERIDO")),
    req("403 · Licitador sin Premium usa LicitAsist", "POST", "/api/asistente/chat", auth="licitador2Jwt", body=chat("Resume mis postulaciones"),
        tests=status(403) + code_is("PREMIUM_REQUERIDO")),
    req("401 · LicitAsist sin autenticación", "POST", "/api/asistente/chat", body=chat("hola"), tests=status(401)),
    req("400 · LicitAsist con mensaje vacío", "POST", "/api/asistente/chat", auth="adminJwt", body={"mensaje": ""}, tests=status(400)),
    req("Administrador usa LicitAsist (datos reales)", "POST", "/api/asistente/chat", auth="adminJwt",
        body=chat("Resume el estado general de la plataforma: usuarios por rol, licitaciones por estado y pagos."),
        tests=status(200) + [
            'const r = pm.response.json();',
            'pm.test("Respuesta del modelo", () => { pm.expect(r.respuesta).to.be.a("string").and.not.empty; pm.expect(r.modelo).to.eql("openai/gpt-oss-120b"); });',
            'pm.test("Usó fuentes reales", () => pm.expect(r.fuentes.length).to.be.above(0));']),
    req("Estado LicitAsist · Administrador", "GET", "/api/asistente/estado", auth="adminJwt",
        tests=status(200) + json_eq(".acceso", True)),
    req("Pyme Premium usa LicitAsist", "POST", "/api/asistente/chat", auth="pymeJwt",
        body=chat("¿Qué licitaciones están abiertas y cuáles cierran esta semana?"),
        pre=SKIP_SI_NO_PREMIUM("pymeJwt"),
        tests=status(200) + ['pm.test("Respuesta", () => pm.expect(pm.response.json().respuesta).to.be.a("string").and.not.empty);']),
    req("Licitador Premium usa LicitAsist", "POST", "/api/asistente/chat", auth="licitadorJwt",
        body=chat("Resume y compara las postulaciones de mi licitación", "{{licitacionId}}"),
        pre=SKIP_SI_NO_PREMIUM("licitadorJwt"),
        tests=status(200)),
])

# --------------------------------------------------------------------------- 09 Admin
admin = folder("09 · Panel administrativo", [
    req("Admin · Listar licitaciones (todas)", "GET", "/api/admin/licitaciones?page=0&size=20", auth="adminJwt", tests=status(200)),
    req("Admin · Filtrar licitaciones por estado", "GET", "/api/admin/licitaciones?estado=ADJUDICADA", auth="adminJwt",
        tests=status(200) + ['pm.test("Solo ADJUDICADA", () => pm.response.json().content.forEach(l => pm.expect(l.estado).to.eql("ADJUDICADA")));']),
    req("Admin · Moderar licitación (SUSPENDER)", "PATCH", "/api/admin/licitaciones/{{licitacion2Id}}/moderar", auth="adminJwt",
        body={"accion": "SUSPENDER", "motivo": "Revisión de contenido"}, tests=status(200) + json_eq(".estado", "SUSPENDIDA")),
    req("Licitación suspendida no aparece a las Pymes", "GET", "/api/licitaciones/{{licitacion2Id}}", auth="pymeJwt",
        tests=['pm.test("404 para pymes sin postulación / visible si postuló", () => pm.expect([200,404]).to.include(pm.response.code));']),
    req("Admin · Moderar licitación (REACTIVAR)", "PATCH", "/api/admin/licitaciones/{{licitacion2Id}}/moderar", auth="adminJwt",
        body={"accion": "REACTIVAR"}, tests=status(200) + json_eq(".estado", "PUBLICADA")),
    req("409 · Admin reactiva licitación no suspendida", "PATCH", "/api/admin/licitaciones/{{licitacion2Id}}/moderar", auth="adminJwt",
        body={"accion": "REACTIVAR"}, tests=status(409)),
    req("Admin · Consultar postulaciones", "GET", "/api/admin/postulaciones?licitacionId={{licitacionId}}", auth="adminJwt",
        tests=status(200) + ['pm.test("2 postulaciones", () => pm.expect(pm.response.json().totalElements).to.eql(2));']),
    req("Admin · Estadísticas de licitaciones", "GET", "/api/admin/licitaciones/estadisticas", auth="adminJwt", tests=status(200)),
    req("Admin · Consultar ventas", "GET", "/api/admin/ventas", auth="adminJwt", tests=status(200)),
    req("Admin · Consultar pagos", "GET", "/api/admin/pagos?estado=ANULADO", auth="adminJwt",
        tests=status(200) + ['pm.test("Pagos ANULADO", () => pm.response.json().content.forEach(p => pm.expect(p.estado).to.eql("ANULADO")));']),
    req("Admin · Consultar suscripciones", "GET", "/api/admin/suscripciones?plan=PREMIUM", auth="adminJwt", tests=status(200)),
    req("Admin · Estadísticas de ventas", "GET", "/api/admin/ventas/estadisticas", auth="adminJwt",
        tests=status(200) + ['pm.test("Ingresos y pagos por estado", () => { pm.expect(pm.response.json()).to.have.property("ingresosAprobados"); pm.expect(pm.response.json()).to.have.property("pagosPorEstado"); });']),
    req("Admin · Eliminar licitación (moderación)", "DELETE", "/api/admin/licitaciones/{{licitacion2Id}}", auth="adminJwt", tests=status(204)),
])

# --------------------------------------------------------------------------- 10 Seguridad
seguridad = folder("10 · Seguridad (401 / 403 / 400 / 404 / 5xx / 429)", [
    req("401 · Sin JWT", "GET", "/api/usuarios/me", tests=status(401) + code_is("NO_AUTENTICADO")),
    req("401 · JWT inválido", "GET", "/api/usuarios/me", headers={"Authorization": "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIn0.firmaFalsa"},
        tests=status(401) + code_is("TOKEN_INVALIDO")),
    req("401 · JWT expirado", "GET", "/api/usuarios/me", headers={"Authorization": "Bearer {{jwtExpirado}}"},
        desc="Requiere la variable `jwtSecret` (el mismo JWT_SECRET del .env) para firmar un token ya expirado. Si está vacía, se omite.",
        pre=[
            "const secret = pm.environment.get('jwtSecret');",
            "if (!secret) { console.log('Omitido: define jwtSecret para generar un JWT expirado'); pm.execution.skipRequest(); }",
            "else {",
            "  const CryptoJS = require('crypto-js');",
            "  const b64 = w => CryptoJS.enc.Base64.stringify(w).replace(/=+$/, '').replace(/\\+/g, '-').replace(/\\//g, '_');",
            "  const header = b64(CryptoJS.enc.Utf8.parse(JSON.stringify({alg: 'HS512'})));",
            "  const now = Math.floor(Date.now() / 1000);",
            "  const payload = b64(CryptoJS.enc.Utf8.parse(JSON.stringify({sub: '1', rol: 'PYME', iat: now - 7200, exp: now - 3600})));",
            "  const firma = b64(CryptoJS.HmacSHA512(header + '.' + payload, secret));",
            "  pm.environment.set('jwtExpirado', header + '.' + payload + '.' + firma);",
            "}",
        ],
        tests=status(401) + code_is("TOKEN_EXPIRADO")),
    req("403 · Acceso administrativo con usuario normal", "GET", "/api/admin/usuarios", auth="pymeJwt",
        tests=status(403) + code_is("ACCESO_DENEGADO")),
    req("403 · Licitador al panel de ventas admin", "GET", "/api/admin/pagos", auth="licitadorJwt", tests=status(403)),
    req("400 · Intento de SQL injection en búsqueda", "GET", "/api/licitaciones?q=1' OR '1'='1", auth="pymeJwt",
        tests=status(400) + code_is("SOLICITUD_BLOQUEADA")),
    req("400 · SQL injection UNION SELECT", "GET", "/api/licitaciones?q=x UNION SELECT email FROM usuario", auth="pymeJwt",
        tests=status(400)),
    req("404 · Endpoints internos no expuestos por el Gateway", "GET", "/internal/usuarios/1", headers={"X-Internal-Key": "adivinanza"},
        tests=status(404)),
    req("401 · Endpoint interno directo sin API key", "GET", "/internal/usuarios/1", base="usersUrl", tests=status(401)),
    req("404 · Ruta inexistente", "GET", "/api/no-existe", auth="pymeJwt", tests=status(404)),
    req("5xx · Formato de error de servidor sin stack trace", "GET", "/api/asistente/estado", auth="pymeJwt",
        desc="Para ver un 5xx real detén API Asistente (puerto 8085) y ejecuta esta request: el Gateway responde 503 SERVICIO_NO_DISPONIBLE en JSON. Con el servicio arriba responde 200. En ningún caso se exponen stack traces.",
        tests=['pm.test("200, o 5xx con JSON controlado", () => {',
               '  if (pm.response.code >= 500) { pm.expect(["SERVICIO_NO_DISPONIBLE","ERROR_INTERNO","TIEMPO_AGOTADO"]).to.include(pm.response.json().code); }',
               '  else { pm.expect(pm.response.code).to.eql(200); }',
               '});']),
    req("429 · Rate limiting en login (último test: bloquea el login ~1 min)", "POST", "/api/auth/login",
        body={"email": "ratelimit@licitawatch.test", "password": "Incorrecta1"},
        desc="El pre-request envía intentos de login seguidos hasta que el Gateway (Bucket4j, RATE_LIMIT_AUTH_PER_MINUTE=30 por IP) empieza a responder 429; luego esta request confirma el 429 con Retry-After.",
        pre=[
            "const url = pm.environment.get('gatewayUrl') + '/api/auth/login';",
            "let n = 0;",
            "// Se detiene al primer 429 (la ventana de 1 minuto puede renovarse a mitad de camino)",
            "const enviar = () => pm.sendRequest({url, method: 'POST', header: {'Content-Type': 'application/json'},",
            "  body: {mode: 'raw', raw: JSON.stringify({email: 'ratelimit@licitawatch.test', password: 'x'})}}, (e, r) => {",
            "  n++;",
            "  if (r && r.code === 429) { console.log('Rate limit alcanzado tras ' + n + ' intentos'); return; }",
            "  if (n < 100) enviar();",
            "});",
            "enviar();",
        ],
        tests=status(429) + code_is("DEMASIADAS_SOLICITUDES") + ['pm.test("Retry-After", () => pm.expect(pm.response.headers.get("Retry-After")).to.exist);']),
])

collection = {
    "info": {
        "_postman_id": str(uuid.uuid5(uuid.NAMESPACE_URL, "licitawatch-collection")),
        "name": "LicitaWatch · Sistema completo",
        "description": (
            "Colección completa de LicitaWatch (proyecto Capstone). Ejecutar EN ORDEN con el Collection Runner o Newman "
            "contra el sistema real (Gateway 8080 + 5 microservicios + PostgreSQL). Los JWT e IDs se guardan automáticamente "
            "en el environment. Webpay Plus corre en SANDBOX de Transbank (sin cobros reales).\n\n"
            "Variables opcionales: `emailPrueba` (buzón que recibe los correos mediante alias +tag), `confirmToken` "
            "(token del correo de confirmación), `pagoCompletado=si` (tras pagar en Webpay sandbox), `jwtSecret` (para el test de JWT expirado)."
        ),
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "event": [{
        "listen": "test",
        "script": {"type": "text/javascript", "exec": [
            "// Verificación global: ninguna respuesta expone stack traces, hashes ni secretos",
            "pm.test('Sin stack traces ni datos sensibles', () => {",
            "  const t = pm.response.text();",
            "  ['at cl.licitawatch', 'java.lang.', 'passwordHash', 'password_hash', 'X-Internal-Key', 'GROQ_API_KEY'].forEach(s => pm.expect(t).to.not.include(s));",
            "});",
        ]},
    }],
    "item": [setup, auth, usuarios, licitaciones, postulaciones, planes, ventas, notificaciones, licitasist, admin, seguridad],
    "variable": [
        {"key": "baseUrl", "value": "http://localhost:8080"},
    ],
}

env_values = [
    ("baseUrl", "http://localhost:8080"), ("gatewayUrl", "http://localhost:8080"),
    ("usersUrl", "http://localhost:8081"), ("licitacionesUrl", "http://localhost:8082"),
    ("ventasUrl", "http://localhost:8083"), ("notificacionesUrl", "http://localhost:8084"),
    ("asistenteUrl", "http://localhost:8085"),
    ("adminEmail", "admin@licitawatch.local"), ("adminPassword", "AdminLicitawatch2026!"),
    ("emailPrueba", "licitawatch@gmail.com"),
    ("pymeEmail", ""), ("pymePassword", "Pyme12345"), ("licitadorEmail", ""), ("licitadorPassword", "Licita12345"),
    ("jwt", ""), ("pymeJwt", ""), ("licitadorJwt", ""), ("adminJwt", ""), ("pyme2Jwt", ""), ("licitador2Jwt", ""),
    ("userId", ""), ("pymeId", ""), ("licitadorId", ""), ("pymeUserId", ""), ("licitadorUserId", ""),
    ("licitacionId", ""), ("licitacion2Id", ""), ("postulacionId", ""), ("postulacion2Id", ""),
    ("planId", ""), ("planEstandarId", ""), ("suscripcionId", ""), ("ventaId", ""), ("pagoId", ""),
    ("webpayToken", ""), ("webpayUrl", ""), ("buyOrder", ""), ("notificacionId", ""),
    ("confirmToken", ""), ("pagoCompletado", "no"), ("jwtSecret", ""), ("fechaCierre", ""), ("runId", ""),
]
environment = {
    "id": str(uuid.uuid5(uuid.NAMESPACE_URL, "licitawatch-environment")),
    "name": "LicitaWatch · Local",
    "values": [{"key": k, "value": v, "type": "secret" if k in ("adminPassword", "jwtSecret") or k.endswith("Jwt") or k == "jwt" else "default",
                "enabled": True} for k, v in env_values],
    "_postman_variable_scope": "environment",
}


def contar(items):
    return sum(contar(i["item"]) if "item" in i else 1 for i in items)


os.makedirs(OUT, exist_ok=True)
with open(os.path.join(OUT, "LicitaWatch.postman_collection.json"), "w", encoding="utf-8") as f:
    json.dump(collection, f, ensure_ascii=False, indent=2)
with open(os.path.join(OUT, "LicitaWatch.postman_environment.json"), "w", encoding="utf-8") as f:
    json.dump(environment, f, ensure_ascii=False, indent=2)
print(f"Colección generada: {contar(collection['item'])} requests en {len(collection['item'])} carpetas")


# =========================================================================== COLECCIÓN DE EVIDENCIAS
# Mismas requests y pruebas, pero con los DATOS ENVIADOS VISIBLES (literales) en URL y body,
# para capturas de evidencia. Solo los IDs generados por el servidor y los JWT siguen siendo variables.
# Cada vez que se regenera usa un sello nuevo (emails y RUT nuevos) para poder ejecutarla desde cero.
import copy  # noqa: E402
import random  # noqa: E402
import re  # noqa: E402
from datetime import date, datetime, timedelta  # noqa: E402


def rut_valido(rnd):
    cuerpo = str(rnd.randint(10_000_000, 99_999_999))
    s, m = 0, 2
    for d in reversed(cuerpo):
        s += int(d) * m
        m = 2 if m == 7 else m + 1
    r = 11 - s % 11
    return f"{cuerpo}-{'0' if r == 11 else 'K' if r == 10 else r}"


sello = datetime.now().strftime("%m%d%H%M")
rnd = random.Random(sello)
correo = lambda tag: f"licitawatch+{tag}{sello}@gmail.com"  # noqa: E731
LITERALES = {
    # URLs
    "gatewayUrl": "http://localhost:8080", "baseUrl": "http://localhost:8080", "usersUrl": "http://localhost:8081",
    "licitacionesUrl": "http://localhost:8082", "ventasUrl": "http://localhost:8083",
    "notificacionesUrl": "http://localhost:8084", "asistenteUrl": "http://localhost:8085",
    # Credenciales y datos de prueba
    "adminEmail": "admin@licitawatch.local", "adminPassword": "AdminLicitawatch2026!", "runId": sello,
    "regpymeMail": correo("regpyme"), "reglicitadorMail": correo("reglicitador"),
    "pymeEmail": correo("pyme"), "pyme2Mail": correo("pyme2"), "licitadorEmail": correo("licitador"),
    "licitador2Mail": correo("licitador2"), "temporalMail": correo("temporal"),
    "pymeRut": rut_valido(rnd), "pyme2Rut": rut_valido(rnd), "licitadorRut": rut_valido(rnd),
    "licitador2Rut": rut_valido(rnd), "regPymeRut": rut_valido(rnd), "regLicitadorRut": rut_valido(rnd),
    "tempRut": rut_valido(rnd), "fechaCierre": (date.today() + timedelta(days=20)).isoformat(),
}
PATRON_VAR = re.compile(r"\{\{(" + "|".join(map(re.escape, LITERALES)) + r")\}\}")


def literalizar(v):
    if isinstance(v, str):
        return PATRON_VAR.sub(lambda m: LITERALES[m.group(1)], v)
    if isinstance(v, list):
        return [literalizar(x) for x in v]
    if isinstance(v, dict):
        return {k: (v[k] if k == "exec" else literalizar(v[k])) for k in v}
    return v


evidencias = literalizar(copy.deepcopy(collection))
evidencias["info"]["_postman_id"] = str(uuid.uuid5(uuid.NAMESPACE_URL, "licitawatch-evidencias-" + sello))
evidencias["info"]["name"] = f"LicitaWatch · Evidencias (datos visibles) {sello}"
evidencias["info"]["description"] = (
    "Colección de EVIDENCIAS: mismas requests y pruebas que 'LicitaWatch · Sistema completo', pero con los datos enviados "
    "visibles en la URL y el body (emails, RUT, contraseñas de prueba, montos, fechas). Solo los IDs que genera el servidor "
    "y los JWT se mantienen como variables porque cambian en cada ejecución (se ven resueltos en la Consola de Postman). "
    f"Sello de esta generación: {sello}. Para volver a ejecutarla desde cero (sin 409 por datos repetidos) regenera con "
    "`python scripts/dev/build_postman.py` e impórtala de nuevo. Usar con el environment 'LicitaWatch · Local'."
)
# El primer request fija en el environment los mismos literales (los tests que comparan valores siguen funcionando)
primer = evidencias["item"][0]["item"][0]
for ev in primer["event"]:
    if ev["listen"] == "prerequest":
        ev["script"]["exec"] = ["// Datos fijos de esta colección de evidencias (visibles en cada request)"] + [
            f"pm.environment.set('{k}', '{v}');" for k, v in LITERALES.items()]
# Evidencia en consola: request enviado y respuesta recibida
evidencias["event"][0]["script"]["exec"] += [
    "console.log('▶ REQUEST', pm.request.method, pm.request.url.toString(), pm.request.body ? pm.request.body.raw : '');",
    "console.log('◀ RESPONSE', pm.response.code, pm.response.status, pm.response.text());",
]

with open(os.path.join(OUT, "LicitaWatch-Evidencias.postman_collection.json"), "w", encoding="utf-8") as f:
    json.dump(evidencias, f, ensure_ascii=False, indent=2)
restantes = sorted(set(re.findall(r"\{\{(\w+)\}\}", json.dumps(
    [i for i in evidencias["item"]], ensure_ascii=False))))
print(f"Colección de evidencias generada (sello {sello}). Variables que quedan (IDs/JWT del servidor): {', '.join(restantes)}")
