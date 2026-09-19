package com.ronda.backend.perfil;

import com.ronda.backend.publicacion.EstadoPublicacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unico lugar donde se calcula la reputacion de un usuario.
 * Cuando existan las features de calificaciones (punto 9) y ofertas (punto 7),
 * solo hay que cambiar esta clase: los controladores y las pantallas ya consumen
 * el record Reputacion y no se enteran del cambio.
 */
@Service
public class ReputacionService {
    private final PerfilPublicacionRepository publicaciones;

    public ReputacionService(PerfilPublicacionRepository publicaciones) {
        this.publicaciones = publicaciones;
    }

    @Transactional(readOnly = true)
    public PerfilDtos.Reputacion calcular(Long usuarioId) {
        // Ventas: publicaciones del usuario marcadas como VENDIDA.
        long ventas = publicaciones.countByVendedorIdAndEstadoPublicacion(usuarioId, EstadoPublicacion.VENDIDA);

        // TODO punto 9: promedio y cantidad desde la tabla de calificaciones.
        Double promedioEstrellas = null;
        long cantidadCalificaciones = 0;

        // TODO punto 7: compras concretadas a partir de las ofertas aceptadas como comprador.
        long compras = 0;

        return new PerfilDtos.Reputacion(promedioEstrellas, cantidadCalificaciones, ventas, compras);
    }
}
