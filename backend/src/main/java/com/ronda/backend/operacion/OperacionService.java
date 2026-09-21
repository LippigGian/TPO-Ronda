package com.ronda.backend.operacion;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import com.ronda.backend.publicacion.EstadoPublicacion;
import com.ronda.backend.publicacion.Publicacion;
import com.ronda.backend.publicacion.PublicacionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperacionService {
    private final OperacionRepository operaciones;
    private final PublicacionRepository publicaciones;
    private final UsuarioRepository usuarios;
    private final CalificacionRepository calificaciones;

    public OperacionService(OperacionRepository operaciones, PublicacionRepository publicaciones,
                             UsuarioRepository usuarios, CalificacionRepository calificaciones) {
        this.operaciones = operaciones;
        this.publicaciones = publicaciones;
        this.usuarios = usuarios;
        this.calificaciones = calificaciones;
    }

    @Transactional
    public OperacionDtos.HistorialItemResponse vender(String email, Long publicacionId, OperacionDtos.VenderRequest request) {
        Usuario vendedor = findUser(email);
        Publicacion publicacion = publicaciones.findByIdAndVendedorId(publicacionId, vendedor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));

        if (publicacion.getEstadoPublicacion() == EstadoPublicacion.VENDIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La publicación ya fue vendida");
        }

        Usuario comprador = usuarios.findByEmailIgnoreCase(request.compradorEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró un usuario con ese email"));

        if (comprador.getId().equals(vendedor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comprador no puede ser el mismo vendedor");
        }

        publicacion.cambiarEstado(EstadoPublicacion.VENDIDA);
        Operacion operacion = operaciones.save(new Operacion(publicacion, vendedor, comprador, request.montoFinal()));

        return toHistorialItem(operacion, vendedor.getId());
    }

    @Transactional(readOnly = true)
    public List<OperacionDtos.HistorialItemResponse> historial(String email, TipoOperacion tipo, Instant desde, Instant hasta) {
        Usuario usuario = findUser(email);
        return operaciones.findByVendedorIdOrCompradorIdOrderByFechaOperacionDesc(usuario.getId(), usuario.getId()).stream()
                .filter(op -> coincideTipo(op, usuario.getId(), tipo))
                .filter(op -> desde == null || !op.getFechaOperacion().isBefore(desde))
                .filter(op -> hasta == null || !op.getFechaOperacion().isAfter(hasta))
                .map(op -> toHistorialItem(op, usuario.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OperacionDtos.HistorialItemResponse> pendientesCalificar(String email) {
        Usuario usuario = findUser(email);
        Instant ahora = Instant.now();
        return operaciones.findByVendedorIdOrCompradorIdOrderByFechaOperacionDesc(usuario.getId(), usuario.getId()).stream()
                .filter(op -> op.puedeCalificar(ahora))
                .filter(op -> !calificaciones.existsByOperacionIdAndAutorId(op.getId(), usuario.getId()))
                .map(op -> toHistorialItem(op, usuario.getId()))
                .toList();
    }

    private boolean coincideTipo(Operacion operacion, Long usuarioId, TipoOperacion tipo) {
        if (tipo == null || tipo == TipoOperacion.TODAS) {
            return true;
        }
        boolean esVenta = operacion.getVendedor().getId().equals(usuarioId);
        return tipo == TipoOperacion.VENTA ? esVenta : !esVenta;
    }

    private OperacionDtos.HistorialItemResponse toHistorialItem(Operacion operacion, Long usuarioId) {
        boolean esVenta = operacion.getVendedor().getId().equals(usuarioId);
        boolean yaCalificada = calificaciones.existsByOperacionIdAndAutorId(operacion.getId(), usuarioId);
        Instant limite = operacion.getFechaOperacion().plus(Operacion.VENTANA_CALIFICACION);
        boolean puedeCalificar = !yaCalificada && Instant.now().isBefore(limite);

        Usuario contraparte = operacion.contraparte(usuarioId);
        return new OperacionDtos.HistorialItemResponse(
                operacion.getId(),
                esVenta ? "VENTA" : "COMPRA",
                operacion.getPublicacion().getId(),
                operacion.getPublicacion().getTitulo(),
                operacion.getMontoFinal(),
                operacion.getFechaOperacion(),
                contraparte.getId(),
                contraparte.getNombreUsuario(),
                yaCalificada,
                puedeCalificar ? limite : null);
    }

    private Usuario findUser(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }
}
