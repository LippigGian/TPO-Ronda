package com.example.ronda.perfil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Textos derivados del perfil que se muestran en mas de una pantalla.
 * Centralizarlos garantiza que "Mi perfil" y el perfil publico se vean igual.
 */
public final class PerfilFormatter {
    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");

    private PerfilFormatter() { }

    /** Ej: "★ 4.5 (12 calificaciones)" o "Sin calificaciones aún". */
    public static String estrellas(PerfilModels.Reputacion reputacion) {
        if (reputacion == null || reputacion.getPromedioEstrellas() == null) {
            return "Sin calificaciones aún";
        }
        return String.format(ES_AR, "★ %.1f (%d calificaciones)",
                reputacion.getPromedioEstrellas(), reputacion.getCantidadCalificaciones());
    }

    /** Ej: "3 ventas · 1 compra". */
    public static String operaciones(PerfilModels.Reputacion reputacion) {
        long ventas = reputacion == null ? 0 : reputacion.getVentasConcretadas();
        long compras = reputacion == null ? 0 : reputacion.getComprasConcretadas();
        return ventas + (ventas == 1 ? " venta" : " ventas")
                + " · " + compras + (compras == 1 ? " compra" : " compras");
    }

    /**
     * El backend manda fechas ISO-8601 (ej: "2026-09-19T14:29:23.456Z").
     * Se usa SimpleDateFormat porque java.time requiere API 26 y la app soporta desde la 24.
     */
    public static String miembroDesde(String fechaIso) {
        if (fechaIso == null || fechaIso.length() < 10) {
            return "";
        }
        try {
            Date fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fechaIso.substring(0, 10));
            return "En Ronda desde " + new SimpleDateFormat("MMMM 'de' yyyy", ES_AR).format(fecha);
        } catch (ParseException e) {
            return "";
        }
    }

    public static String precio(double precio) {
        return String.format(ES_AR, "$ %,.2f", precio);
    }

    /** Convierte NUEVO / COMO_NUEVO / USADO en texto legible. */
    public static String estadoArticulo(String estado) {
        if (estado == null) {
            return "";
        }
        switch (estado) {
            case "NUEVO": return "Nuevo";
            case "COMO_NUEVO": return "Como nuevo";
            case "USADO": return "Usado";
            default: return estado;
        }
    }
}
