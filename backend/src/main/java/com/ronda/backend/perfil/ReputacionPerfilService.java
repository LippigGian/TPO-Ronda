package com.ronda.backend.perfil;

import com.ronda.backend.operacion.CalificacionRepository;
import com.ronda.backend.operacion.OperacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unico lugar donde se calcula la reputacion de un usuario (perfil propio, perfil publico
 * y datos del vendedor en el detalle de una publicacion).
 * Se llama ReputacionPerfilService (y no ReputacionService) para no chocar con el bean
 * homonimo de la feature de calificaciones (punto 9): Spring no admite dos beans con el
 * mismo nombre y el backend no arrancaria al mergear ambas ramas.
 */
@Service
public class ReputacionPerfilService {
    private final OperacionRepository operaciones;
    private final CalificacionRepository calificaciones;

    public ReputacionPerfilService(OperacionRepository operaciones, CalificacionRepository calificaciones) {
        this.operaciones = operaciones;
        this.calificaciones = calificaciones;
    }

    @Transactional(readOnly = true)
    public PerfilDtos.Reputacion calcular(Long usuarioId) {
        long ventas = operaciones.countByVendedorId(usuarioId);
        long compras = operaciones.countByCompradorId(usuarioId);

        Double promedioEstrellas = calificaciones.promedioPuntaje(usuarioId);
        long cantidadCalificaciones = calificaciones.countByReceptorId(usuarioId);

        return new PerfilDtos.Reputacion(promedioEstrellas, cantidadCalificaciones, ventas, compras);
    }
}
