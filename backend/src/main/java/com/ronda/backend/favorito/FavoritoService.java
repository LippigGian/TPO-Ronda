package com.ronda.backend.favorito;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import com.ronda.backend.publicacion.Publicacion;
import com.ronda.backend.publicacion.PublicacionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FavoritoService {
    private final FavoritoRepository favoritos;
    private final PublicacionRepository publicaciones;
    private final UsuarioRepository usuarios;

    public FavoritoService(FavoritoRepository favoritos, PublicacionRepository publicaciones,
                           UsuarioRepository usuarios) {
        this.favoritos = favoritos;
        this.publicaciones = publicaciones;
        this.usuarios = usuarios;
    }

    /** Idempotente: si ya era favorita, no hace nada. */
    @Transactional
    public void marcar(String email, Long publicacionId) {
        Usuario usuario = findUser(email);
        if (favoritos.findByUsuarioIdAndPublicacionId(usuario.getId(), publicacionId).isPresent()) {
            return;
        }
        Publicacion publicacion = publicaciones.findById(publicacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));
        favoritos.save(new Favorito(usuario, publicacion));
    }

    /** Idempotente: si no era favorita, no hace nada. */
    @Transactional
    public void desmarcar(String email, Long publicacionId) {
        Usuario usuario = findUser(email);
        favoritos.deleteByUsuarioIdAndPublicacionId(usuario.getId(), publicacionId);
    }

    @Transactional(readOnly = true)
    public List<FavoritoDtos.Item> listar(String email) {
        Usuario usuario = findUser(email);
        return favoritos.findByUsuarioIdOrderByCreatedAtDesc(usuario.getId()).stream()
                .map(FavoritoDtos.Item::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> idsFavoritos(String email) {
        Usuario usuario = findUser(email);
        return favoritos.findPublicacionIdsByUsuarioId(usuario.getId());
    }

    /** Actualiza el snapshot de precio al abrir el detalle; sin efecto si no es favorita. */
    @Transactional
    public void marcarVisto(String email, Long publicacionId) {
        Usuario usuario = findUser(email);
        favoritos.findByUsuarioIdAndPublicacionId(usuario.getId(), publicacionId)
                .ifPresent(Favorito::marcarVisto);
    }

    private Usuario findUser(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }
}
