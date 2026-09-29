-- PPT diap. 13 · postulacion(id PK, licitacion_id FK -> licitacion, pyme_id ref. Usuarios,
--                            fecha_postulacion date, estado enum)
-- pyme_id NO es FK: es el id del perfil pyme en API Usuarios, validado por REST.
CREATE TYPE postulacion_estado AS ENUM ('ENVIADA', 'EN_REVISION', 'SELECCIONADA', 'RECHAZADA', 'RETIRADA');

CREATE TABLE postulacion (
    id                BIGSERIAL          PRIMARY KEY,
    licitacion_id     BIGINT             NOT NULL REFERENCES licitacion (id),
    pyme_id           BIGINT             NOT NULL,
    fecha_postulacion DATE               NOT NULL,
    estado            postulacion_estado NOT NULL
);
