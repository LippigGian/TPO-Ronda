package com.ronda.backend.publicacion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long>, JpaSpecificationExecutor<Publicacion> {
    List<Publicacion> findByVendedorIdOrderByCreatedAtDesc(Long vendedorId);

    @Query("select distinct p.categoria from Publicacion p where p.estadoPublicacion = com.ronda.backend.publicacion.EstadoPublicacion.ACTIVA order by p.categoria")
    List<String> findCategoriasActivas();

    Optional<Publicacion> findByIdAndVendedorId(Long id, Long vendedorId);
}
