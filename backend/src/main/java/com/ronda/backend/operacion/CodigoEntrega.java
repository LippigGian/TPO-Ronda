package com.ronda.backend.operacion;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formato del QR fisico de entrega: "RONDA-ENTREGA-{idPublicacion}", por ejemplo "RONDA-ENTREGA-12".
 * El QR no lo genera la app; lo imprime el vendedor y lo escanea el comprador al recibir el articulo.
 * Se toleran espacios alrededor y minusculas, porque los generadores de QR a veces los agregan.
 */
final class CodigoEntrega {
    private static final Pattern FORMATO = Pattern.compile("^RONDA-ENTREGA-(\\d{1,18})$", Pattern.CASE_INSENSITIVE);

    private CodigoEntrega() { }

    static Optional<Long> publicacionId(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        Matcher matcher = FORMATO.matcher(codigo.strip());
        return matcher.matches() ? Optional.of(Long.parseLong(matcher.group(1))) : Optional.empty();
    }
}
