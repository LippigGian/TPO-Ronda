package com.ronda.backend.operacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class OperacionDtos {
    private OperacionDtos() { }

    /** Texto leido del QR fisico de la entrega, por ejemplo "RONDA-ENTREGA-12". */
    public record ConfirmarRecepcionRequest(
            @NotBlank(message = "Código inválido: el QR está vacío")
            @Size(max = 100, message = "Código inválido: no es un QR de entrega de Ronda") String codigo) { }

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
