-- Avisos y notificaciones (punto 11 del TPO).
-- Cada fila es un aviso para un usuario: una oferta recibida o respondida, o una pregunta
-- recibida o respondida. La app las consulta y las muestra como notificacion del sistema;
-- "entregada" evita mostrar dos veces la misma y "leida" alimenta la bandeja de avisos.
CREATE TABLE ronda.notificaciones (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES ronda.usuarios(id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    mensaje VARCHAR(300) NOT NULL,
    -- Destino al tocar el aviso: la oferta si existe, si no la publicacion.
    publicacion_id BIGINT REFERENCES ronda.publicaciones(id) ON DELETE CASCADE,
    oferta_id BIGINT REFERENCES ronda.ofertas(id) ON DELETE CASCADE,
    entregada BOOLEAN NOT NULL DEFAULT FALSE,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_notificaciones_tipo CHECK (tipo IN ('OFERTA_RECIBIDA', 'CONTRAOFERTA_RECIBIDA', 'OFERTA_ACEPTADA',
        'OFERTA_RECHAZADA', 'PREGUNTA_RECIBIDA', 'PREGUNTA_RESPONDIDA'))
);

-- La app pide las no entregadas de un usuario cada pocos segundos.
CREATE INDEX idx_notificaciones_pendientes ON ronda.notificaciones (usuario_id, id) WHERE entregada = FALSE;
-- Bandeja: las ultimas del usuario.
CREATE INDEX idx_notificaciones_usuario_id ON ronda.notificaciones (usuario_id, id DESC);
