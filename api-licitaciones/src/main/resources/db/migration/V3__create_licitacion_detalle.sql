-- Tabla de SOPORTE 1:1 con licitacion (no altera la tabla oficial de la PPT):
-- descripción, requisitos, condiciones, categoría y datos de publicación.
CREATE TABLE licitacion_detalle (
    licitacion_id     BIGINT       PRIMARY KEY REFERENCES licitacion (id),
    descripcion       TEXT         NOT NULL,
    requisitos        TEXT,
    condiciones       TEXT,
    categoria         VARCHAR(80)  NOT NULL,
    region            VARCHAR(80),
    destacada         BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_publicacion TIMESTAMP,
    creado_en         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
