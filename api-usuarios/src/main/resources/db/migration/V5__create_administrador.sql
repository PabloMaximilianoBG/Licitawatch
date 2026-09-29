-- PPT diap. 12 · administrador(id PK, usuario_id FK/UNIQUE, area texto)
CREATE TABLE administrador (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT    NOT NULL UNIQUE REFERENCES usuario (id),
    area       TEXT      NOT NULL
);
