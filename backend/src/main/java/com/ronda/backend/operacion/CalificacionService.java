package com.ronda.backend.operacion;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CalificacionService {
    private final OperacionRepository operaciones;
    private final CalificacionRepository calificaciones;
    private final UsuarioRepository usuarios;

    public CalificacionService(OperacionRepository operaciones, CalificacionRepository calificaciones, UsuarioRepository usuarios) {
        this.operaciones = operaciones;
        this.calificaciones = calificaciones;
        this.usuarios = usuarios;
    }

    @Transactional
    public CalificacionDtos.Response crear(String email, Long operacionId, CalificacionDtos.CreateRequest request) {
        Usuario autor = findUser(email);
        Operacion operacion = operaciones.findById(operacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operación no encontrada"));

        if (!operacion.esParte(autor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Operación no encontrada");
        }

        if (!operacion.puedeCalificar(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya pasaron los 7 días para calificar esta operación");
        }

        if (calificaciones.existsByOperacionIdAndAutorId(operacionId, autor.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya calificaste esta operación");
        }

        Usuario receptor = operacion.contraparte(autor.getId());
        Calificacion calificacion = calificaciones.save(
                new Calificacion(operacion, autor, receptor, request.puntaje(), request.comentario()));

        return CalificacionDtos.Response.from(calificacion);
    }

    private Usuario findUser(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }
}
