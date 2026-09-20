package com.ronda.backend.perfil;

import com.ronda.backend.oferta.EstadoOferta;
import com.ronda.backend.oferta.OfertaRepository;
import com.ronda.backend.publicacion.EstadoPublicacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unico lugar donde se calcula la reputacion de un usuario (perfil propio, perfil publico
 * y datos del vendedor en el detalle de una publicacion).
 * Se llama ReputacionPerfilService (y no ReputacionService) para no chocar con el bean
 * homonimo de la feature de calificaciones (punto 9): Spring no admite dos beans con el
 * mismo nombre y el backend no arrancaria al mergear ambas ramas.
 * Cuando se integre la feature de calificaciones (punto 9), solo hay que cambiar esta
 * clase: los controladores y las pantallas ya consumen el record Reputacion.
 */
@Service
public class ReputacionPerfilService {
    private final PerfilPublicacionRepository publicaciones;
    private final OfertaRepository ofertas;

    public ReputacionPerfilService(PerfilPublicacionRepository publicaciones, OfertaRepository ofertas) {
        this.publicaciones = publicaciones;
        this.ofertas = ofertas;
    }

    @Transactional(readOnly = true)
    public PerfilDtos.Reputacion calcular(Long usuarioId) {
        // Ventas: publicaciones del usuario marcadas como VENDIDA.
        long ventas = publicaciones.countByVendedorIdAndEstadoPublicacion(usuarioId, EstadoPublicacion.VENDIDA);

        // TODO punto 9: promedio y cantidad desde la tabla de calificaciones.
        Double promedioEstrellas = null;
        long cantidadCalificaciones = 0;

        // Compras: ofertas aceptadas como comprador sobre publicaciones que ya se vendieron.
        long compras = ofertas.countByCompradorIdAndEstadoAndPublicacionEstadoPublicacion(usuarioId,
                EstadoOferta.ACEPTADA, EstadoPublicacion.VENDIDA);

        return new PerfilDtos.Reputacion(promedioEstrellas, cantidadCalificaciones, ventas, compras);
    }
}
