package com.ronda.backend.publicacion;

import java.math.BigDecimal;

/** Filtros combinables del Home. Todos son opcionales; la cercanía necesita lat, lng y radioKm juntos. */
public record ExplorarFiltros(String q, String categoria, BigDecimal precioMin, BigDecimal precioMax,
                              EstadoArticulo estadoArticulo, Double lat, Double lng, Double radioKm) {

    public boolean filtraPorCercania() {
        return lat != null && lng != null && radioKm != null && radioKm > 0;
    }
}
