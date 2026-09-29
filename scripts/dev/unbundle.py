"""Herramienta de desarrollo: separa un archivo "bundle" en múltiples archivos.

Formato del bundle:
    === FILE: ruta/relativa/Archivo.java
    <contenido>
    === FILE: otra/ruta.txt
    <contenido>

Uso: python scripts/dev/unbundle.py <bundle> <directorio_base>
"""
import os
import sys

MARK = "=== FILE: "


def main():
    bundle, base = sys.argv[1], sys.argv[2]
    with open(bundle, encoding="utf-8") as f:
        lines = f.read().split("\n")
    current, buf, count = None, [], 0

    def flush():
        nonlocal count
        if current is None:
            return
        path = os.path.join(base, current)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        text = "\n".join(buf).rstrip("\n") + "\n"
        with open(path, "w", encoding="utf-8", newline="\n") as out:
            out.write(text)
        count += 1

    for line in lines:
        if line.startswith(MARK):
            flush()
            current, buf = line[len(MARK):].strip(), []
        else:
            buf.append(line)
    flush()
    print(f"{count} archivos escritos en {base}")


if __name__ == "__main__":
    main()
