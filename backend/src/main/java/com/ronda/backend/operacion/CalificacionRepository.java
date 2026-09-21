package com.ronda.backend.operacion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {
    boolean existsByOperacionIdAndAutorId(Long operacionId, Long autorId);
    List<Calificacion> findByReceptorIdOrderByCreatedAtDesc(Long receptorId);
    long countByReceptorId(Long receptorId);

    @Query("SELECT AVG(c.puntaje) FROM Calificacion c WHERE c.receptor.id = :receptorId")
    Double promedioPuntaje(@Param("receptorId") Long receptorId);
}
