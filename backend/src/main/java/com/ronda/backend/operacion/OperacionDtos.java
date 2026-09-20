package com.ronda.backend.operacion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public final class OperacionDtos {
    private OperacionDtos() { }

    public record VenderRequest(
            @NotBlank @Email String compradorEmail,
            @NotNull @DecimalMin(value = "0.01") BigDecimal montoFinal) { }

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
