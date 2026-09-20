package com.ronda.backend.operacion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class CalificacionDtos {
    private CalificacionDtos() { }

    public record CreateRequest(
            @NotNull @Min(1) @Max(5) Integer puntaje,
            @Size(max = 500) String comentario) { }

    public record Response(
            Long id,
            Long operacionId,
            String autorNombre,
            String receptorNombre,
            int puntaje,
            String comentario,
            Instant createdAt) {
        static Response from(Calificacion calificacion) {
            return new Response(
                    calificacion.getId(),
                    calificacion.getOperacion().getId(),
                    calificacion.getAutor().getNombreUsuario(),
                    calificacion.getReceptor().getNombreUsuario(),
                    calificacion.getPuntaje(),
                    calificacion.getComentario(),
                    calificacion.getCreatedAt());
        }
    }
}
