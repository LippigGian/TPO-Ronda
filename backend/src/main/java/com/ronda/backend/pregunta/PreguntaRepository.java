package com.ronda.backend.pregunta;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {
    List<Pregunta> findByPublicacionIdOrderByIdAsc(Long publicacionId);
}
