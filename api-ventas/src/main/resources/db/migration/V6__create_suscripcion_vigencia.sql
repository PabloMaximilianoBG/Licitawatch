-- Tabla de SOPORTE 1:1 con suscripcion: período de vigencia.
CREATE TABLE suscripcion_vigencia (
    suscripcion_id BIGINT    PRIMARY KEY REFERENCES suscripcion (id),
    fecha_inicio   TIMESTAMP,
    fecha_fin      TIMESTAMP,
    creado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_suscripcion_usuario ON suscripcion (usuario_id, estado);
