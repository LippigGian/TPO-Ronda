package com.example.ronda;

/** Filtros combinados del Home. Un valor null significa "sin filtrar por ese criterio". */
public final class FiltrosHome {
    public static final double RADIO_KM_POR_DEFECTO = 10.0;

    public String categoria;
    public Double precioMin;
    public Double precioMax;
    /** NUEVO, COMO_NUEVO o USADO. */
    public String estadoArticulo;
    public boolean cercania;
    public double radioKm = RADIO_KM_POR_DEFECTO;

    public boolean hayFiltrosActivos() {
        return categoria != null || precioMin != null || precioMax != null
                || estadoArticulo != null || cercania;
    }

    public int cantidadActivos() {
        int cantidad = 0;
        if (categoria != null) cantidad++;
        if (precioMin != null || precioMax != null) cantidad++;
        if (estadoArticulo != null) cantidad++;
        if (cercania) cantidad++;
        return cantidad;
    }
}
