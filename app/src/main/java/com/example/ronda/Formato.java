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
}
