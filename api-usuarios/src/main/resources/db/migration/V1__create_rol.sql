-- PPT diap. 12 · rol(id PK, nombre enum)
CREATE TYPE rol_nombre AS ENUM ('LICITADOR', 'PYME', 'ADMINISTRADOR');

CREATE TABLE rol (
    id     BIGSERIAL  PRIMARY KEY,
    nombre rol_nombre NOT NULL
);

INSERT INTO rol (nombre) VALUES ('LICITADOR'), ('PYME'), ('ADMINISTRADOR');
