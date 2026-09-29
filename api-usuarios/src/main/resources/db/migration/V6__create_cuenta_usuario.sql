-- Tabla de SOPORTE (no forma parte del modelo conceptual de la PPT).
-- Guarda los datos técnicos de autenticación del usuario base, 1:1 con usuario,
-- para no alterar la tabla oficial "usuario" (id, email, rol_id, activo).
-- La contraseña se guarda SOLO como hash BCrypt.
CREATE TABLE cuenta_usuario (
    usuario_id       BIGINT       PRIMARY KEY REFERENCES usuario (id),
    password_hash    VARCHAR(100) NOT NULL,
    email_confirmado BOOLEAN      NOT NULL DEFAULT FALSE,
    nombre           VARCHAR(150),
    creado_en        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_login     TIMESTAMP
);
