-- Tabla de SOPORTE 1:1 con pago: datos devueltos por Webpay Plus.
-- NUNCA se almacena el número completo de la tarjeta ni el CVV: solo los 4 últimos dígitos que entrega Transbank.
CREATE TABLE pago_webpay (
    pago_id             BIGINT       PRIMARY KEY REFERENCES pago (id),
    buy_order           VARCHAR(26)  NOT NULL UNIQUE,
    session_id          VARCHAR(61)  NOT NULL,
    token               VARCHAR(64)  UNIQUE,
    url_pago            VARCHAR(255),
    authorization_code  VARCHAR(20),
    card_last_digits    VARCHAR(4),
    response_code       INTEGER,
    payment_type_code   VARCHAR(4),
    installments_number INTEGER,
    transaction_date    TIMESTAMP,
    vci                 VARCHAR(10),
    status_webpay       VARCHAR(40),
    comprobante_enviado BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
