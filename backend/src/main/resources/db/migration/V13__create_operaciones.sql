CREATE TABLE ronda.operaciones (
    id BIGSERIAL PRIMARY KEY,
    publicacion_id BIGINT NOT NULL REFERENCES ronda.publicaciones(id),
    vendedor_id BIGINT NOT NULL REFERENCES ronda.usuarios(id),
    comprador_id BIGINT NOT NULL REFERENCES ronda.usuarios(id),
    monto_final NUMERIC(12, 2) NOT NULL CHECK (monto_final > 0),
    fecha_operacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_operaciones_publicacion UNIQUE (publicacion_id),
    CONSTRAINT ck_operaciones_partes_distintas CHECK (comprador_id <> vendedor_id)
);

CREATE INDEX idx_operaciones_vendedor_fecha
    ON ronda.operaciones (vendedor_id, fecha_operacion DESC);

CREATE INDEX idx_operaciones_comprador_fecha
    ON ronda.operaciones (comprador_id, fecha_operacion DESC);
