package com.ronda.backend.notificacion;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** Avisos que la app todavia no mostro. Se bloquean para que dos consultas simultaneas no los repitan. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Notificacion> findByUsuarioIdAndEntregadaFalseOrderByIdAsc(Long usuarioId);

    /** Bandeja: los ultimos avisos del usuario. */
    List<Notificacion> findTop50ByUsuarioIdOrderByIdDesc(Long usuarioId);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    @Modifying(flushAutomatically = true)
    @Query("update Notificacion n set n.leida = true where n.usuario.id = :usuarioId and n.leida = false")
    int marcarTodasLeidas(@Param("usuarioId") Long usuarioId);
}
