package com.example.ronda.oferta;

import com.example.ronda.Formato;
import com.example.ronda.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Textos y colores de una oferta. Centralizados para que se vean igual en toda la app. */
public final class OfertaFormatter {
    private static final long MINUTO_MS = 60_000L;
    private static final long HORA_MS = 60 * MINUTO_MS;
    private static final long DIA_MS = 24 * HORA_MS;

    private OfertaFormatter() { }

    public static String estado(String estado) {
        if (estado == null) {
            return "";
        }
        switch (estado) {
            case OfertaModels.PENDIENTE: return "Pendiente";
            case OfertaModels.ACEPTADA: return "Aceptada";
            case OfertaModels.RECHAZADA: return "Rechazada";
            case OfertaModels.VENCIDA: return "Vencida";
            default: return estado;
        }
    }

    /** Color del texto del chip de estado. */
    public static int colorEstado(String estado) {
        if (OfertaModels.ACEPTADA.equals(estado)) return R.color.oferta_aceptada;
        if (OfertaModels.RECHAZADA.equals(estado)) return R.color.oferta_rechazada;
        if (OfertaModels.VENCIDA.equals(estado)) return R.color.oferta_vencida;
        return R.color.oferta_pendiente;
    }

    /** Color de fondo del chip de estado. */
    public static int fondoEstado(String estado) {
        if (OfertaModels.ACEPTADA.equals(estado)) return R.color.oferta_aceptada_fondo;
        if (OfertaModels.RECHAZADA.equals(estado)) return R.color.oferta_rechazada_fondo;
        if (OfertaModels.VENCIDA.equals(estado)) return R.color.oferta_vencida_fondo;
        return R.color.oferta_pendiente_fondo;
    }

    /** Ej: "Propuesta: $ 90.000 · Publicado: $ 100.000". */
    public static String precios(OfertaModels.Oferta oferta) {
        return "Propuesta: " + Formato.precio(oferta.getPrecio())
                + " · Publicado: " + Formato.precio(oferta.getPublicacion().getPrecioPublicado());
    }

    /** Ej: "Vendedor: Juan" o "Comprador: Ana". */
    public static String contraparte(OfertaModels.Oferta oferta) {
        String rol = oferta.soyComprador() ? "Vendedor: " : "Comprador: ";
        return rol + oferta.getContraparte().getNombre();
    }

    /** Frase que explica en qué punto está la negociación y qué tiene que hacer el usuario. */
    public static String situacion(OfertaModels.Oferta oferta) {
        switch (oferta.getEstado()) {
            case OfertaModels.ACEPTADA:
                return oferta.soyComprador()
                        ? "¡Trato cerrado! Ya podés ver el punto de entrega."
                        : "Trato cerrado. El comprador ya puede ver el punto de entrega.";
            case OfertaModels.RECHAZADA:
                return "La negociación terminó sin acuerdo.";
            case OfertaModels.VENCIDA:
                return "La oferta venció sin respuesta.";
            default:
                String plazo = tiempoRestante(oferta.getVenceAt());
                return oferta.isPuedoResponder()
                        ? "Te toca responder · " + plazo
                        : "Esperando respuesta de " + oferta.getContraparte().getNombre() + " · " + plazo;
        }
    }

    /**
     * "vence en 1 d 4 h", "vence en 3 h 10 min", "vence en 5 min".
     * El backend manda fechas ISO-8601 en UTC; se usa SimpleDateFormat porque java.time requiere API 26.
     */
    public static String tiempoRestante(String venceAtIso) {
        Date vence = parsearUtc(venceAtIso);
        if (vence == null) {
            return "";
        }
        long restante = vence.getTime() - System.currentTimeMillis();
        if (restante <= 0) {
            return "vence en instantes";
        }
        if (restante >= DIA_MS) {
            return "vence en " + (restante / DIA_MS) + " d " + ((restante % DIA_MS) / HORA_MS) + " h";
        }
        if (restante >= HORA_MS) {
            return "vence en " + (restante / HORA_MS) + " h " + ((restante % HORA_MS) / MINUTO_MS) + " min";
        }
        return restante >= MINUTO_MS ? "vence en " + (restante / MINUTO_MS) + " min" : "vence en menos de 1 min";
    }

    private static Date parsearUtc(String iso) {
        if (iso == null || iso.length() < 19) {
            return null;
        }
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return formato.parse(iso.substring(0, 19));
        } catch (ParseException e) {
            return null;
        }
    }
}
