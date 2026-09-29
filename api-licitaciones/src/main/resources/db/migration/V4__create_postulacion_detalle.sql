-- Tabla de SOPORTE 1:1 con postulacion: propuesta de la pyme y evaluación del licitador.
CREATE TABLE postulacion_detalle (
    postulacion_id       BIGINT         PRIMARY KEY REFERENCES postulacion (id),
    propuesta            TEXT           NOT NULL,
    monto_ofertado       NUMERIC(15, 2) NOT NULL,
    plazo_entrega_dias   INTEGER        NOT NULL,
    comentario_licitador TEXT,
    creado_en            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
