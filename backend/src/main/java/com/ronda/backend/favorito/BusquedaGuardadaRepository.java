package com.ronda.backend.favorito;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusquedaGuardadaRepository extends JpaRepository<BusquedaGuardada, Long> {
    List<BusquedaGuardada> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    Optional<BusquedaGuardada> findByIdAndUsuarioId(Long id, Long usuarioId);
}
