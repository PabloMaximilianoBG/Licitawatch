-- PPT diap. 12 · usuario(id PK, email UNIQUE, rol_id FK -> rol, activo bool)
CREATE TABLE usuario (
    id     BIGSERIAL    PRIMARY KEY,
    email  VARCHAR(255) NOT NULL UNIQUE,
    rol_id BIGINT       NOT NULL REFERENCES rol (id),
    activo BOOLEAN      NOT NULL DEFAULT TRUE
);
