package com.ronda.backend.pregunta;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import com.ronda.backend.notificacion.NotificacionService;
import com.ronda.backend.perfil.PerfilService;
import com.ronda.backend.publicacion.EstadoPublicacion;
import com.ronda.backend.publicacion.Publicacion;
import com.ronda.backend.publicacion.PublicacionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Punto 4: preguntas de los interesados y respuestas del vendedor en el detalle de una publicación.
 * Punto 11: cada pregunta avisa al vendedor y cada respuesta avisa a quien preguntó.
 */
@Service
public class PreguntaService {
    private final PreguntaRepository preguntas;
    private final PublicacionRepository publicaciones;
    private final UsuarioRepository usuarios;
    private final PerfilService perfiles;
    private final NotificacionService notificaciones;

    public PreguntaService(PreguntaRepository preguntas, PublicacionRepository publicaciones,
                           UsuarioRepository usuarios, PerfilService perfiles,
                           NotificacionService notificaciones) {
        this.preguntas = preguntas;
        this.publicaciones = publicaciones;
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.notificaciones = notificaciones;
    }

    @Transactional(readOnly = true)
    public List<PreguntaDtos.Response> listar(String email, Long publicacionId) {
        Usuario usuario = usuarioPorEmail(email);
        buscarPublicacion(publicacionId);
        return preguntas.findByPublicacionIdOrderByIdAsc(publicacionId).stream()
                .map(pregunta -> aResponse(pregunta, usuario))
                .toList();
    }

    @Transactional
    public PreguntaDtos.Response preguntar(String email, Long publicacionId, PreguntaDtos.PreguntarRequest request) {
        Usuario autor = usuarioPorEmail(email);
        Publicacion publicacion = buscarPublicacion(publicacionId);
        if (publicacion.getVendedor().getId().equals(autor.getId())) {
            throw error(HttpStatus.BAD_REQUEST, "No podés preguntar en tu propia publicación");
        }
        if (publicacion.getEstadoPublicacion() != EstadoPublicacion.ACTIVA) {
            throw error(HttpStatus.CONFLICT, "La publicación no está disponible para recibir preguntas");
        }
        String texto = request.texto().trim();
        if (texto.length() < 3) {
            throw error(HttpStatus.BAD_REQUEST, "La pregunta debe tener entre 3 y 500 caracteres");
        }

        Pregunta pregunta = preguntas.save(new Pregunta(publicacion, autor, texto));
        // Punto 11: aviso al vendedor.
        notificaciones.preguntaRecibida(publicacion.getVendedor(), perfiles.nombrePublico(autor),
                publicacion.getTitulo(), texto, publicacion.getId());
        return aResponse(pregunta, autor);
    }

    @Transactional
    public PreguntaDtos.Response responder(String email, Long preguntaId, PreguntaDtos.ResponderRequest request) {
        Usuario vendedor = usuarioPorEmail(email);
        Pregunta pregunta = preguntas.findById(preguntaId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Pregunta no encontrada"));
        Publicacion publicacion = pregunta.getPublicacion();
        if (!publicacion.getVendedor().getId().equals(vendedor.getId())) {
            throw error(HttpStatus.FORBIDDEN, "Solo el vendedor puede responder las preguntas");
        }
        if (pregunta.estaRespondida()) {
            throw error(HttpStatus.CONFLICT, "Esta pregunta ya fue respondida");
        }

        String respuesta = request.respuesta().trim();
        pregunta.responder(respuesta);
        // Punto 11: aviso a quien preguntó.
        notificaciones.preguntaRespondida(pregunta.getAutor(), publicacion.getTitulo(), respuesta, publicacion.getId());
        return aResponse(pregunta, vendedor);
    }

    private PreguntaDtos.Response aResponse(Pregunta pregunta, Usuario usuario) {
        boolean soyVendedor = pregunta.getPublicacion().getVendedor().getId().equals(usuario.getId());
        return new PreguntaDtos.Response(pregunta.getId(), pregunta.getTexto(),
                perfiles.nombrePublico(pregunta.getAutor()), pregunta.getAutor().getId().equals(usuario.getId()),
                pregunta.getRespuesta(), pregunta.getCreatedAt(), pregunta.getRespondidaAt(),
                soyVendedor && !pregunta.estaRespondida());
    }

    private Publicacion buscarPublicacion(Long id) {
        return publicaciones.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Publicación no encontrada"));
    }

    private Usuario usuarioPorEmail(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }

    private static ResponseStatusException error(HttpStatus status, String mensaje) {
        return new ResponseStatusException(status, mensaje);
    }
}
