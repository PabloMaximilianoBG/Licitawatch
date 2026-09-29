-- PPT diap. 15 · notificacion(id PK, usuario_id ref. Usuarios, tipo enum, canal email)
-- usuario_id NO es FK: id del usuario en API Usuarios. Sin relaciones con otras tablas propias.
CREATE TYPE notificacion_tipo AS ENUM (
    'CONFIRMACION_REGISTRO',
    'LICITACION_PUBLICADA',
    'POSTULACION_RECIBIDA',
    'POSTULACION_ENVIADA',
    'RESULTADO_POSTULACION',
    'LICITACION_ADJUDICADA',
    'PAGO_CONFIRMADO',
    'PAGO_RECHAZADO',
    'GENERAL'
);
CREATE TYPE notificacion_canal AS ENUM ('EMAIL');

CREATE TABLE notificacion (
    id         BIGSERIAL          PRIMARY KEY,
    usuario_id BIGINT             NOT NULL,
    tipo       notificacion_tipo  NOT NULL,
    canal      notificacion_canal NOT NULL
);

CREATE INDEX idx_notificacion_usuario ON notificacion (usuario_id);
