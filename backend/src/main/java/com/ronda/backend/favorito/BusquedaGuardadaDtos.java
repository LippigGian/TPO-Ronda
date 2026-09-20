package com.ronda.backend.favorito;

import com.ronda.backend.publicacion.EstadoArticulo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class BusquedaGuardadaDtos {
    private BusquedaGuardadaDtos() { }

    /** Mismos filtros combinables del Home (ExplorarFiltros), más el nombre para identificarla. */
    public record CrearRequest(
            @NotBlank @Size(max = 80) String nombre,
            String q,
            String categoria,
            BigDecimal precioMin,
            BigDecimal precioMax,
            EstadoArticulo estadoArticulo,
            Double lat,
            Double lng,
            Double radioKm) { }

    public record Item(Long id, String nombre, String q, String categoria, BigDecimal precioMin,
                       BigDecimal precioMax, EstadoArticulo estadoArticulo, Double radioKm,
                       long cantidadNuevas, Instant createdAt) {
        static Item from(BusquedaGuardada busqueda, long cantidadNuevas) {
            var filtros = busqueda.aFiltros();
            return new Item(busqueda.getId(), busqueda.getNombre(), filtros.q(), filtros.categoria(),
                    filtros.precioMin(), filtros.precioMax(), filtros.estadoArticulo(), filtros.radioKm(),
                    cantidadNuevas, busqueda.getCreatedAt());
        }
    }
}
