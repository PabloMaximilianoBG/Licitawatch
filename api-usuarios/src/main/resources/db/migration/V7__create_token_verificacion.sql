-- Tabla de SOPORTE: tokens de confirmación de correo.
-- Solo se almacena el hash SHA-256 del token (el token en claro viaja únicamente en el correo).
CREATE TABLE token_verificacion (
    id         BIGSERIAL   PRIMARY KEY,
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    tipo       VARCHAR(30) NOT NULL,
    expira_en  TIMESTAMP   NOT NULL,
    usado_en   TIMESTAMP,
    creado_en  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_token_verificacion_usuario ON token_verificacion (usuario_id);
