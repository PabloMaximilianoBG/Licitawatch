-- PPT diap. 12 · pyme(id PK, usuario_id FK/UNIQUE, nombre_contacto texto, rut UNIQUE)
CREATE TABLE pyme (
    id              BIGSERIAL   PRIMARY KEY,
    usuario_id      BIGINT      NOT NULL UNIQUE REFERENCES usuario (id),
    nombre_contacto TEXT        NOT NULL,
    rut             VARCHAR(12) NOT NULL UNIQUE
);
