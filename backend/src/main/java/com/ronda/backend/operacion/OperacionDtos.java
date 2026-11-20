package com.ronda.backend.operacion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public final class OperacionDtos {
    private OperacionDtos() { }

    /**
     * Datos del QR fisico de entrega, ya parseados por la app. El QR contiene un JSON como
     * {"tipo": "RONDA-ENTREGA", "publicacionId": 12, "titulo": "Bici"}; la app lo lee, lo valida
     * y envia solo el id de la publicacion.
     */
    public record ConfirmarRecepcionRequest(
            @NotNull(message = "Código inválido: el QR no indica la publicación")
            @Positive(message = "Código inválido: no es un QR de entrega de Ronda") Long publicacionId) { }

    public record HistorialItemResponse(
            Long id,
            String tipo,
            Long publicacionId,
            String publicacionTitulo,
            BigDecimal montoFinal,
            Instant fechaOperacion,
            Long contraparteId,
            String contraparteNombre,
            boolean yaCalificada,
            Instant puedeCalificarHasta) { }
}
