-- Ofertas y negociacion (punto 7 del TPO).
-- Una fila = una negociacion entre un comprador y el vendedor de una publicacion.
-- "precio" es siempre la ultima propuesta; "turno" indica quien tiene que responder.
CREATE TABLE ronda.ofertas (
    id BIGSERIAL PRIMARY KEY,
    publicacion_id BIGINT NOT NULL REFERENCES ronda.publicaciones(id) ON DELETE CASCADE,
    comprador_id BIGINT NOT NULL REFERENCES ronda.usuarios(id),
    precio NUMERIC(12, 2) NOT NULL CHECK (precio > 0),
    mensaje VARCHAR(280),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    turno VARCHAR(20) NOT NULL DEFAULT 'VENDEDOR',
    vence_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_ofertas_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'VENCIDA')),
    CONSTRAINT ck_ofertas_turno CHECK (turno IN ('COMPRADOR', 'VENDEDOR'))
);

-- "Mis ofertas": las enviadas se buscan por comprador y las recibidas por publicacion.
CREATE INDEX idx_ofertas_comprador_updated_at ON ronda.ofertas (comprador_id, updated_at DESC);
CREATE INDEX idx_ofertas_publicacion_estado ON ronda.ofertas (publicacion_id, estado);

-- Vencimiento automatico: busca pendientes con plazo cumplido.
CREATE INDEX idx_ofertas_pendientes_vence_at ON ronda.ofertas (vence_at) WHERE estado = 'PENDIENTE';

-- Un comprador no puede tener dos negociaciones abiertas sobre la misma publicacion.
CREATE UNIQUE INDEX uk_ofertas_una_pendiente_por_comprador
    ON ronda.ofertas (publicacion_id, comprador_id) WHERE estado = 'PENDIENTE';
