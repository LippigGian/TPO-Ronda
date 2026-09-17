CREATE TABLE ronda.publicacion_fotos (
    id BIGSERIAL PRIMARY KEY,
    publicacion_id BIGINT NOT NULL REFERENCES ronda.publicaciones(id) ON DELETE CASCADE,
    archivo VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    orden INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_publicacion_fotos_archivo UNIQUE (archivo),
    CONSTRAINT uk_publicacion_fotos_orden UNIQUE (publicacion_id, orden)
);

CREATE INDEX idx_publicacion_fotos_publicacion_orden
    ON ronda.publicacion_fotos (publicacion_id, orden);
