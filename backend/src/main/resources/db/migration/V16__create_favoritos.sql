CREATE TABLE ronda.favoritos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES ronda.usuarios(id) ON DELETE CASCADE,
    publicacion_id BIGINT NOT NULL REFERENCES ronda.publicaciones(id) ON DELETE CASCADE,
    precio_guardado NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_favoritos_usuario_publicacion UNIQUE (usuario_id, publicacion_id)
);

CREATE INDEX idx_favoritos_usuario_fecha ON ronda.favoritos (usuario_id, created_at DESC);
