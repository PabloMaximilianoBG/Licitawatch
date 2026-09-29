-- PPT diap. 14 · venta(id PK, suscripcion_id FK -> suscripcion, monto numeric, fecha date)
CREATE TABLE venta (
    id             BIGSERIAL      PRIMARY KEY,
    suscripcion_id BIGINT         NOT NULL REFERENCES suscripcion (id),
    monto          NUMERIC(12, 2) NOT NULL,
    fecha          DATE           NOT NULL
);
