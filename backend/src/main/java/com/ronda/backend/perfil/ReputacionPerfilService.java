package com.ronda.backend.perfil;

import com.ronda.backend.publicacion.EstadoPublicacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unico lugar donde se calcula la reputacion de un usuario (perfil propio, perfil publico
 * y datos del vendedor en el detalle de una publicacion).
 * Se llama ReputacionPerfilService (y no ReputacionService) para no chocar con el bean
 * homonimo de la feature de calificaciones (punto 9): Spring no admite dos beans con el
 * mismo nombre y el backend no arrancaria al mergear ambas ramas.
 * Cuando existan las features de calificaciones (punto 9) y ofertas (punto 7),
 * solo hay que cambiar esta clase: los controladores y las pantallas ya consumen
 * el record Reputacion y no se enteran del cambio.
 */
@Service
public class ReputacionPerfilService {
    private final PerfilPublicacionRepository publicaciones;

    public ReputacionPerfilService(PerfilPublicacionRepository publicaciones) {
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
