package com.ronda.backend.operacion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacionRepository extends JpaRepository<Operacion, Long> {
    Optional<Operacion> findByPublicacionId(Long publicacionId);
    List<Operacion> findByVendedorIdOrCompradorIdOrderByFechaOperacionDesc(Long vendedorId, Long compradorId);
}
