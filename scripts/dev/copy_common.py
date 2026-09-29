"""Herramienta de desarrollo: copia las clases transversales (JWT, errores, filtro interno)
desde api-usuarios a otro microservicio, cambiando el paquete.

Uso: python scripts/dev/copy_common.py <carpeta-servicio> <paquete>
Ej:  python scripts/dev/copy_common.py api-licitaciones licitaciones
"""
import os
import sys

SRC = "api-usuarios/src/main/java/cl/licitawatch/usuarios"
FILES = [
    "dto/ErrorResponse.java",
    "dto/PageResponse.java",
    "exception/ApiException.java",
    "exception/GlobalExceptionHandler.java",
    "security/AuthUser.java",
    "security/JwtService.java",
    "security/JwtAuthenticationFilter.java",
    "security/InternalApiKeyFilter.java",
    "security/RestSecurityHandlers.java",
]


def main():
    servicio, paquete = sys.argv[1], sys.argv[2]
    dst_base = f"{servicio}/src/main/java/cl/licitawatch/{paquete}"
    for rel in FILES:
        with open(os.path.join(SRC, rel), encoding="utf-8") as f:
            txt = f.read().replace("cl.licitawatch.usuarios", f"cl.licitawatch.{paquete}")
        dst = os.path.join(dst_base, rel)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(dst, "w", encoding="utf-8", newline="\n") as f:
            f.write(txt)
    print(f"{len(FILES)} archivos comunes copiados a {dst_base}")


if __name__ == "__main__":
    main()
