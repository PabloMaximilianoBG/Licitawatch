-- PPT diap. 14 · pago(id PK, venta_id FK/UNIQUE, id_transaccion, pasarela, estado enum)
CREATE TYPE pago_estado AS ENUM ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'ANULADO');

CREATE TABLE pago (
    id             BIGSERIAL    PRIMARY KEY,
    venta_id       BIGINT       NOT NULL UNIQUE REFERENCES venta (id),
    id_transaccion VARCHAR(100),
    pasarela       VARCHAR(40)  NOT NULL,
    estado         pago_estado  NOT NULL
);
