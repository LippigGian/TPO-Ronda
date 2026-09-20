package com.ronda.backend.operacion;

import java.util.List;

public final class ReputacionDtos {
    private ReputacionDtos() { }

    public record Response(
            Long usuarioId,
            String nombreUsuario,
            double promedio,
            long cantidadCalificaciones,
            List<CalificacionDtos.Response> ultimasCalificaciones) { }
}
