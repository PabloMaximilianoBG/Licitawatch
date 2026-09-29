-- Índices de rendimiento e integridad (no agregan columnas a las tablas oficiales).
-- Una pyme postula una sola vez a cada licitación.
CREATE UNIQUE INDEX uq_postulacion_licitacion_pyme ON postulacion (licitacion_id, pyme_id);
CREATE INDEX idx_licitacion_estado_cierre ON licitacion (estado, fecha_cierre);
CREATE INDEX idx_licitacion_licitador ON licitacion (licitador_id);
CREATE INDEX idx_postulacion_pyme ON postulacion (pyme_id);
