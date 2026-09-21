package com.ronda.backend.operacion;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReputacionService {
    private final CalificacionRepository calificaciones;
    private final UsuarioRepository usuarios;

    public ReputacionService(CalificacionRepository calificaciones, UsuarioRepository usuarios) {
        this.calificaciones = calificaciones;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public ReputacionDtos.Response obtener(Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Double promedio = calificaciones.promedioPuntaje(usuarioId);
        long cantidad = calificaciones.countByReceptorId(usuarioId);
        List<CalificacionDtos.Response> ultimas = calificaciones.findByReceptorIdOrderByCreatedAtDesc(usuarioId).stream()
                .map(CalificacionDtos.Response::from)
                .toList();

        return new ReputacionDtos.Response(
                usuario.getId(),
                usuario.getNombreUsuario(),
                promedio == null ? 0.0 : promedio,
                cantidad,
                ultimas);
    }
}
