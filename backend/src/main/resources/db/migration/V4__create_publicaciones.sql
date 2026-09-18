CREATE TABLE ronda.publicaciones (
    id BIGSERIAL PRIMARY KEY,
    vendedor_id BIGINT NOT NULL REFERENCES ronda.usuarios(id),
    titulo VARCHAR(120) NOT NULL,
    descripcion TEXT NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    precio NUMERIC(12, 2) NOT NULL CHECK (precio > 0),
    estado_articulo VARCHAR(20) NOT NULL,
    estado_publicacion VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    direccion VARCHAR(255) NOT NULL,
    latitud NUMERIC(9, 6) NOT NULL,
    longitud NUMERIC(9, 6) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_publicaciones_estado_articulo
        CHECK (estado_articulo IN ('NUEVO', 'COMO_NUEVO', 'USADO')),
    CONSTRAINT ck_publicaciones_estado_publicacion
        CHECK (estado_publicacion IN ('ACTIVA', 'PAUSADA', 'VENDIDA'))
);

CREATE INDEX idx_publicaciones_vendedor_created_at
    ON ronda.publicaciones (vendedor_id, created_at DESC);
