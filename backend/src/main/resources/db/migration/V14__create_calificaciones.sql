CREATE TABLE ronda.calificaciones (
    id BIGSERIAL PRIMARY KEY,
    operacion_id BIGINT NOT NULL REFERENCES ronda.operaciones(id),
    autor_id BIGINT NOT NULL REFERENCES ronda.usuarios(id),
    receptor_id BIGINT NOT NULL REFERENCES ronda.usuarios(id),
    puntaje SMALLINT NOT NULL,
    comentario VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_calificaciones_operacion_autor UNIQUE (operacion_id, autor_id),
    CONSTRAINT ck_calificaciones_puntaje CHECK (puntaje BETWEEN 1 AND 5),
    CONSTRAINT ck_calificaciones_partes_distintas CHECK (autor_id <> receptor_id)
);

CREATE INDEX idx_calificaciones_receptor_fecha
    ON ronda.calificaciones (receptor_id, created_at DESC);
