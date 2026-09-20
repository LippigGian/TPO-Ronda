package com.ronda.backend.perfil;

import com.ronda.backend.publicacion.EstadoPublicacion;
import com.ronda.backend.publicacion.Publicacion;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Consultas de solo lectura sobre publicaciones que necesita el perfil.
 * Vive en este paquete para no modificar PublicacionRepository (de otra feature)
 * y evitar conflictos de merge. Extiende Repository (no JpaRepository) a proposito:
 * desde el perfil nunca se guardan ni borran publicaciones.
 */
public interface PerfilPublicacionRepository extends Repository<Publicacion, Long> {
    List<Publicacion> findByVendedorIdAndEstadoPublicacionOrderByCreatedAtDesc(Long vendedorId,
                                                                               EstadoPublicacion estado);

    long countByVendedorIdAndEstadoPublicacion(Long vendedorId, EstadoPublicacion estado);
}
