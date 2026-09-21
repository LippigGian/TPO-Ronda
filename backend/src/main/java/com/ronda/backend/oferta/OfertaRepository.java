package com.ronda.backend.oferta;

import com.ronda.backend.publicacion.EstadoPublicacion;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    /** Ofertas enviadas: el usuario es el comprador. */
    List<Oferta> findByCompradorIdOrderByUpdatedAtDesc(Long compradorId);

    /** Ofertas recibidas: el usuario es el vendedor de la publicacion. */
    List<Oferta> findByPublicacionVendedorIdOrderByUpdatedAtDesc(Long vendedorId);

    boolean existsByPublicacionIdAndEstado(Long publicacionId, EstadoOferta estado);

    boolean existsByPublicacionIdAndCompradorIdAndEstado(Long publicacionId, Long compradorId, EstadoOferta estado);

    /** Compras concretadas: ofertas aceptadas como comprador sobre publicaciones ya vendidas. */
    long countByCompradorIdAndEstadoAndPublicacionEstadoPublicacion(Long compradorId, EstadoOferta estado,
                                                                    EstadoPublicacion estadoPublicacion);

    /** Pasa a VENCIDA toda oferta pendiente cuyo plazo ya se cumplio. Devuelve cuantas cambiaron. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Oferta o set o.estado = :vencida, o.updatedAt = :ahora
            where o.estado = :pendiente and o.venceAt < :ahora
            """)
    int vencerPendientes(@Param("ahora") Instant ahora,
                         @Param("pendiente") EstadoOferta pendiente,
                         @Param("vencida") EstadoOferta vencida);

    /** Al aceptar una oferta, las demas pendientes de la misma publicacion se rechazan. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Oferta o set o.estado = :rechazada, o.updatedAt = :ahora
            where o.publicacion.id = :publicacionId and o.id <> :ofertaAceptadaId and o.estado = :pendiente
            """)
    int rechazarOtrasPendientes(@Param("publicacionId") Long publicacionId,
                                @Param("ofertaAceptadaId") Long ofertaAceptadaId,
                                @Param("ahora") Instant ahora,
                                @Param("pendiente") EstadoOferta pendiente,
                                @Param("rechazada") EstadoOferta rechazada);
}
