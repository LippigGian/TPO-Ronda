package com.ronda.backend.favorito;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    Optional<Favorito> findByUsuarioIdAndPublicacionId(Long usuarioId, Long publicacionId);

    void deleteByUsuarioIdAndPublicacionId(Long usuarioId, Long publicacionId);

    @EntityGraph(attributePaths = "publicacion")
    List<Favorito> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    @Query("select f.publicacion.id from Favorito f where f.usuario.id = :usuarioId")
    List<Long> findPublicacionIdsByUsuarioId(@Param("usuarioId") Long usuarioId);
}
