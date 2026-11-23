-- Preguntas y respuestas en una publicación (punto 4) con aviso a la otra parte (punto 11).
-- Una pregunta tiene a lo sumo una respuesta, que solo puede dar el vendedor.
CREATE TABLE ronda.preguntas (
    id BIGSERIAL PRIMARY KEY,
    publicacion_id BIGINT NOT NULL REFERENCES ronda.publicaciones(id) ON DELETE CASCADE,
    autor_id BIGINT NOT NULL REFERENCES ronda.usuarios(id) ON DELETE CASCADE,
    texto VARCHAR(500) NOT NULL,
    respuesta VARCHAR(500),
    respondida_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_preguntas_respuesta CHECK ((respuesta IS NULL) = (respondida_at IS NULL))
);

CREATE INDEX idx_preguntas_publicacion_id ON ronda.preguntas (publicacion_id, id);
