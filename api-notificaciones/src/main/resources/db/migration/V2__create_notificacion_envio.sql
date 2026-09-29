-- Tabla de SOPORTE 1:1 con notificacion: resultado del envío SMTP y estado de lectura.
-- No guarda el cuerpo del correo (puede contener enlaces con tokens de un solo uso).
CREATE TABLE notificacion_envio (
    notificacion_id BIGINT       PRIMARY KEY REFERENCES notificacion (id),
    destinatario    VARCHAR(255) NOT NULL,
    asunto          VARCHAR(255) NOT NULL,
    resumen         VARCHAR(500),
    estado          VARCHAR(20)  NOT NULL,
    detalle_error   VARCHAR(500),
    leida           BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviado_en      TIMESTAMP
);
