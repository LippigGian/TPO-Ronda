package com.ronda.backend.operacion;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import com.ronda.backend.oferta.EstadoOferta;
import com.ronda.backend.oferta.Oferta;
import com.ronda.backend.oferta.OfertaRepository;
import com.ronda.backend.publicacion.EstadoPublicacion;
import com.ronda.backend.publicacion.Publicacion;
import com.ronda.backend.publicacion.PublicacionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final OfertaRepository ofertas;

    public OperacionService(OperacionRepository operaciones, PublicacionRepository publicaciones,
                             UsuarioRepository usuarios, CalificacionRepository calificaciones,
                             OfertaRepository ofertas) {
        this.operaciones = operaciones;
        this.publicaciones = publicaciones;
        this.usuarios = usuarios;
        this.calificaciones = calificaciones;
        this.ofertas = ofertas;
    }

    /**
     * Cierre de la operacion con QR: el comprador escanea el QR fisico de la entrega para confirmar
     * que recibio el articulo. Valida el codigo, registra la operacion con el precio de la oferta
     * aceptada y deja la publicacion VENDIDA, todo en la misma transaccion.
     */
    @Transactional
    public OperacionDtos.HistorialItemResponse confirmarRecepcion(String email, OperacionDtos.ConfirmarRecepcionRequest request) {
        Usuario comprador = findUser(email);
        Long publicacionId = CodigoEntrega.publicacionId(request.codigo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Código inválido: no es un QR de entrega de Ronda"));
        Publicacion publicacion = publicaciones.findById(publicacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Código inválido: no corresponde a ninguna publicación"));

        if (publicacion.getEstadoPublicacion() == EstadoPublicacion.VENDIDA) {
            boolean esMiCompra = operaciones.findByPublicacionId(publicacionId)
                    .map(op -> op.getComprador().getId().equals(comprador.getId()))
                    .orElse(false);
            throw esMiCompra
                    ? new ResponseStatusException(HttpStatus.CONFLICT, "Ya confirmaste la recepción de este artículo")
                    : noEsTuCompra();
        }

        Oferta oferta = ofertas.findFirstByPublicacionIdAndCompradorIdAndEstado(
                        publicacionId, comprador.getId(), EstadoOferta.ACEPTADA)
                .orElseThrow(this::noEsTuCompra);

        publicacion.cambiarEstado(EstadoPublicacion.VENDIDA);
        Operacion operacion;
        try {
            operacion = operaciones.saveAndFlush(
                    new Operacion(publicacion, publicacion.getVendedor(), comprador, oferta.getPrecio()));
        } catch (DataIntegrityViolationException carrera) {
            // Dos escaneos simultaneos: la restriccion unica de operaciones.publicacion_id frena el segundo.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya confirmaste la recepción de este artículo");
        }
        return toHistorialItem(operacion, comprador.getId());
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

    private ResponseStatusException noEsTuCompra() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Código inválido: este artículo no corresponde a una compra tuya con oferta aceptada");
    }

    private Usuario findUser(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }
}
