-- Tabla de SOPORTE: registro de búsquedas para el límite mensual del plan Estándar (PPT diap. 7).
CREATE TABLE busqueda_registro (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT    NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_busqueda_usuario_fecha ON busqueda_registro (usuario_id, fecha_hora);
