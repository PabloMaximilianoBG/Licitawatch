-- PPT diap. 14 · suscripcion(id PK, usuario_id ref. Usuarios, plan_id FK -> plan, estado enum)
-- usuario_id NO es FK: id del usuario en API Usuarios, validado por REST.
CREATE TYPE suscripcion_estado AS ENUM ('PENDIENTE', 'ACTIVA', 'VENCIDA', 'CANCELADA');

CREATE TABLE suscripcion (
    id         BIGSERIAL          PRIMARY KEY,
    usuario_id BIGINT             NOT NULL,
    plan_id    BIGINT             NOT NULL REFERENCES plan_suscripcion (id),
    estado     suscripcion_estado NOT NULL
);
