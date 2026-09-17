package com.ronda.backend.publicacion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {
    List<Publicacion> findByVendedorIdOrderByCreatedAtDesc(Long vendedorId);
    Optional<Publicacion> findByIdAndVendedorId(Long id, Long vendedorId);
}
