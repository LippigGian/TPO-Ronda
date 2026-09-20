package com.example.ronda;

import java.text.NumberFormat;
import java.util.Locale;

/** Formateo de datos de publicaciones para mostrar en pantalla. */
public final class Formato {
    private static final Locale AR = Locale.forLanguageTag("es-AR");

    private Formato() { }

    /** 185000.0 -> "$ 185.000" */
    public static String precio(double precio) {
        NumberFormat formato = NumberFormat.getNumberInstance(AR);
        formato.setMaximumFractionDigits(precio % 1 == 0 ? 0 : 2);
        return "$ " + formato.format(precio);
    }

    /** COMO_NUEVO -> "Como nuevo" */
    public static String estadoArticulo(String estado) {
        if (estado == null) {
            return "";
        }
        switch (estado) {
            case "NUEVO":
                return "Nuevo";
            case "COMO_NUEVO":
                return "Como nuevo";
            case "USADO":
                return "Usado";
            default:
                return estado;
        }
    }

    /** "2026-09-20T13:00:00.123Z" -> "20/09/2026" (sin java.time: minSdk 24). */
    public static String fecha(String iso) {
        if (iso == null || iso.length() < 10) {
            return "";
        }
        String[] partes = iso.substring(0, 10).split("-");
        return partes.length == 3 ? partes[2] + "/" + partes[1] + "/" + partes[0] : "";
    }
}
