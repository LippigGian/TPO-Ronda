package com.ronda.backend.notificacion;

import java.time.Instant;
import java.util.List;

/** Contratos JSON de avisos (punto 11). */
public final class NotificacionDtos {
    private NotificacionDtos() { }

    /** Un aviso. Al tocarlo, la app abre la oferta si hay ofertaId; si no, la publicacion. */
    public record Response(Long id, TipoNotificacion tipo, String titulo, String mensaje, Long publicacionId,
                           Long ofertaId, boolean leida, Instant createdAt) {
        static Response from(Notificacion notificacion) {
            return new Response(notificacion.getId(), notificacion.getTipo(), notificacion.getTitulo(),
                    notificacion.getMensaje(), notificacion.getPublicacionId(), notificacion.getOfertaId(),
                    notificacion.isLeida(), notificacion.getCreatedAt());
        }
    }

    public record BandejaResponse(List<Response> notificaciones, long noLeidas) { }
}
