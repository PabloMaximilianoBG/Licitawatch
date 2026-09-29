-- PPT diap. 12 · licitador(id PK, usuario_id FK/UNIQUE, razon_social texto, rut UNIQUE)
CREATE TABLE licitador (
    id           BIGSERIAL   PRIMARY KEY,
    usuario_id   BIGINT      NOT NULL UNIQUE REFERENCES usuario (id),
    razon_social TEXT        NOT NULL,
    rut          VARCHAR(12) NOT NULL UNIQUE
);
