package com.ronda.backend.pregunta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Contratos JSON de preguntas y respuestas. */
public final class PreguntaDtos {
    private PreguntaDtos() { }

    public record PreguntarRequest(
            @NotBlank(message = "Escribí tu pregunta")
            @Size(min = 3, max = 500, message = "La pregunta debe tener entre 3 y 500 caracteres")
            String texto) { }

    public record ResponderRequest(
            @NotBlank(message = "Escribí la respuesta")
            @Size(min = 1, max = 500, message = "La respuesta debe tener hasta 500 caracteres")
            String respuesta) { }

    /**
     * Una pregunta como la ve quien consulta: con el nombre público de quien preguntó,
     * si es suya y si la puede responder (solo el vendedor, mientras no tenga respuesta).
     */
    public record Response(Long id, String texto, String autor, boolean esMia, String respuesta,
                           Instant createdAt, Instant respondidaAt, boolean puedoResponder) { }
}
