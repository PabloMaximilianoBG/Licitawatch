-- PPT diap. 14 · plan_suscripcion(id PK, nombre Estándar/Premium)
CREATE TYPE plan_nombre AS ENUM ('ESTANDAR', 'PREMIUM');

CREATE TABLE plan_suscripcion (
    id     BIGSERIAL   PRIMARY KEY,
    nombre plan_nombre NOT NULL
);

INSERT INTO plan_suscripcion (nombre) VALUES ('ESTANDAR'), ('PREMIUM');
