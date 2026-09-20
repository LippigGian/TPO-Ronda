package com.ronda.backend.favorito;

import com.ronda.backend.publicacion.EstadoPublicacion;
import com.ronda.backend.publicacion.Publicacion;
import java.math.BigDecimal;
import java.time.Instant;

public final class FavoritoDtos {
    private FavoritoDtos() { }

    /** Item de "Mis favoritos". cambioPrecio indica el indicador de novedad por precio. */
    public record Item(Long publicacionId, String titulo, String foto, BigDecimal precioActual,
                       BigDecimal precioGuardado, boolean cambioPrecio, EstadoPublicacion estadoPublicacion,
                       Instant createdAt) {
        static Item from(Favorito favorito) {
            Publicacion publicacion = favorito.getPublicacion();
            String foto = publicacion.getFotos().isEmpty() ? null : "/uploads/" + publicacion.getFotos().get(0).getArchivo();
            return new Item(publicacion.getId(), publicacion.getTitulo(), foto, publicacion.getPrecio(),
                    favorito.getPrecioGuardado(), favorito.cambioPrecio(), publicacion.getEstadoPublicacion(),
                    favorito.getCreatedAt());
        }
    }
}
