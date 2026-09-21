CREATE TABLE ronda.busquedas_guardadas (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES ronda.usuarios(id) ON DELETE CASCADE,
    nombre VARCHAR(80) NOT NULL,
    texto VARCHAR(120),
    categoria VARCHAR(80),
    precio_min NUMERIC(12, 2),
    precio_max NUMERIC(12, 2),
    estado_articulo VARCHAR(20),
    latitud NUMERIC(9, 6),
    longitud NUMERIC(9, 6),
    radio_km NUMERIC(6, 2),
    ultima_revision_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_busquedas_guardadas_precio CHECK (precio_min IS NULL OR precio_max IS NULL OR precio_min <= precio_max),
    CONSTRAINT ck_busquedas_guardadas_estado_articulo
        CHECK (estado_articulo IS NULL OR estado_articulo IN ('NUEVO', 'COMO_NUEVO', 'USADO'))
);

CREATE INDEX idx_busquedas_guardadas_usuario_fecha ON ronda.busquedas_guardadas (usuario_id, created_at DESC);
