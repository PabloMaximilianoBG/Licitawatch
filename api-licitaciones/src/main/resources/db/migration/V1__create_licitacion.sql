-- PPT diap. 13 · licitacion(id PK, licitador_id ref. Usuarios, titulo texto,
--                           monto_estimado numeric, fecha_cierre date, estado enum)
-- licitador_id NO es FK: es el id del perfil licitador en API Usuarios, validado por REST.
CREATE TYPE licitacion_estado AS ENUM ('BORRADOR', 'PUBLICADA', 'CERRADA', 'ADJUDICADA', 'CANCELADA', 'SUSPENDIDA');

CREATE TABLE licitacion (
    id             BIGSERIAL         PRIMARY KEY,
    licitador_id   BIGINT            NOT NULL,
    titulo         TEXT              NOT NULL,
    monto_estimado NUMERIC(15, 2)    NOT NULL,
    fecha_cierre   DATE              NOT NULL,
    estado         licitacion_estado NOT NULL
);
