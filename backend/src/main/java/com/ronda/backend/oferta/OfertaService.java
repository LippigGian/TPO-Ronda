package com.ronda.backend.oferta;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import com.ronda.backend.perfil.PerfilService;
import com.ronda.backend.publicacion.EstadoPublicacion;
import com.ronda.backend.publicacion.Publicacion;
import com.ronda.backend.publicacion.PublicacionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reglas de negocio del punto 7 (Ofertas y Negociacion).
 *
 * Todas las operaciones empiezan venciendo las ofertas expiradas, asi el estado que ve
 * el usuario siempre esta al dia aunque el job periodico todavia no haya corrido.
 */
@Service
public class OfertaService {
    private final OfertaRepository ofertas;
    private final PublicacionRepository publicaciones;
    private final UsuarioRepository usuarios;
    private final PerfilService perfiles;
    private final Duration vigencia;

    public OfertaService(OfertaRepository ofertas, PublicacionRepository publicaciones, UsuarioRepository usuarios,
                         PerfilService perfiles,
                         @Value("${app.ofertas.vigencia-minutos:2880}") long vigenciaMinutos) {
        this.ofertas = ofertas;
        this.publicaciones = publicaciones;
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.vigencia = Duration.ofMinutes(vigenciaMinutos);
    }

    // ---------- Casos de uso ----------

    /** El interesado propone un precio distinto al publicado, con un mensaje opcional. */
    @Transactional
    public OfertaDtos.Response ofertar(String email, Long publicacionId, OfertaDtos.PropuestaRequest request) {
        vencerOfertasExpiradas();
        Usuario comprador = usuarioPorEmail(email);
        Publicacion publicacion = publicaciones.findById(publicacionId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Publicación no encontrada"));

        if (publicacion.getVendedor().getId().equals(comprador.getId())) {
            throw error(HttpStatus.BAD_REQUEST, "No podés ofertar en tu propia publicación");
        }
        if (publicacion.getEstadoPublicacion() != EstadoPublicacion.ACTIVA) {
            throw error(HttpStatus.CONFLICT, "La publicación no está disponible para recibir ofertas");
        }
        if (request.precio().compareTo(publicacion.getPrecio()) == 0) {
            throw error(HttpStatus.BAD_REQUEST, "La oferta tiene que ser por un precio distinto al publicado");
        }
        if (ofertas.existsByPublicacionIdAndEstado(publicacionId, EstadoOferta.ACEPTADA)) {
            throw error(HttpStatus.CONFLICT, "La publicación ya tiene una oferta aceptada");
        }
        if (ofertas.existsByPublicacionIdAndCompradorIdAndEstado(publicacionId, comprador.getId(),
                EstadoOferta.PENDIENTE)) {
            throw error(HttpStatus.CONFLICT, "Ya tenés una oferta pendiente en esta publicación");
        }

        Oferta oferta = ofertas.save(new Oferta(publicacion, comprador, request.precio(),
                limpiar(request.mensaje()), ahora().plus(vigencia)));
        return aResponse(oferta, comprador.getId());
    }

    /** "Mis ofertas": las enviadas y las recibidas, en una sola consulta. */
    @Transactional
    public OfertaDtos.MisOfertasResponse misOfertas(String email) {
        vencerOfertasExpiradas();
        Usuario usuario = usuarioPorEmail(email);

        List<OfertaDtos.Response> enviadas = ofertas.findByCompradorIdOrderByUpdatedAtDesc(usuario.getId()).stream()
                .map(oferta -> aResponse(oferta, usuario.getId()))
                .toList();
        List<OfertaDtos.Response> recibidas = ofertas.findByPublicacionVendedorIdOrderByUpdatedAtDesc(usuario.getId())
                .stream()
                .map(oferta -> aResponse(oferta, usuario.getId()))
                .toList();
        long pendientesDeMiRespuesta = enviadas.stream().filter(OfertaDtos.Response::puedoResponder).count()
                + recibidas.stream().filter(OfertaDtos.Response::puedoResponder).count();

        return new OfertaDtos.MisOfertasResponse(enviadas, recibidas, pendientesDeMiRespuesta);
    }

    @Transactional
    public OfertaDtos.Response obtener(String email, Long ofertaId) {
        vencerOfertasExpiradas();
        Usuario usuario = usuarioPorEmail(email);
        return aResponse(buscarComoParticipante(ofertaId, usuario), usuario.getId());
    }

    @Transactional
    public OfertaDtos.Response aceptar(String email, Long ofertaId) {
        vencerOfertasExpiradas();
        Usuario usuario = usuarioPorEmail(email);
        Oferta oferta = buscarComoParticipante(ofertaId, usuario);
        validarQuePuedeResponder(oferta, usuario);
        if (oferta.getPublicacion().getEstadoPublicacion() != EstadoPublicacion.ACTIVA) {
            throw error(HttpStatus.CONFLICT, "La publicación ya no está activa");
        }

        Instant ahora = ahora();
        oferta.aceptar(ahora);
        // Solo puede haber un comprador: el resto de las negociaciones abiertas se cierran.
        ofertas.rechazarOtrasPendientes(oferta.getPublicacion().getId(), oferta.getId(), ahora,
                EstadoOferta.PENDIENTE, EstadoOferta.RECHAZADA);
        return aResponse(oferta, usuario.getId());
    }

    @Transactional
    public OfertaDtos.Response rechazar(String email, Long ofertaId) {
        vencerOfertasExpiradas();
        Usuario usuario = usuarioPorEmail(email);
        Oferta oferta = buscarComoParticipante(ofertaId, usuario);
        validarQuePuedeResponder(oferta, usuario);

        oferta.rechazar(ahora());
        return aResponse(oferta, usuario.getId());
    }

    /** Nueva propuesta de precio: el turno pasa a la otra parte y el plazo se renueva. */
    @Transactional
    public OfertaDtos.Response contraofertar(String email, Long ofertaId, OfertaDtos.PropuestaRequest request) {
        vencerOfertasExpiradas();
        Usuario usuario = usuarioPorEmail(email);
        Oferta oferta = buscarComoParticipante(ofertaId, usuario);
        validarQuePuedeResponder(oferta, usuario);
        if (request.precio().compareTo(oferta.getPrecio()) == 0) {
            throw error(HttpStatus.BAD_REQUEST, "La contraoferta tiene que proponer un precio distinto");
        }

        Instant ahora = ahora();
        oferta.contraofertar(request.precio(), limpiar(request.mensaje()), ahora.plus(vigencia), ahora);
        return aResponse(oferta, usuario.getId());
    }

    // ---------- Consultas para otras features ----------

    /** Detalle de publicacion (punto 4): el comprador ve la direccion recien con la oferta aceptada. */
    @Transactional(readOnly = true)
    public boolean compradorTieneOfertaAceptada(Long publicacionId, Long compradorId) {
        return ofertas.existsByPublicacionIdAndCompradorIdAndEstado(publicacionId, compradorId,
                EstadoOferta.ACEPTADA);
    }

    /** Caducidad automatica: la usan el job periodico y cada caso de uso de este servicio. */
    @Transactional
    public int vencerOfertasExpiradas() {
        return ofertas.vencerPendientes(ahora(), EstadoOferta.PENDIENTE, EstadoOferta.VENCIDA);
    }

    // ---------- Helpers ----------

    private OfertaDtos.Response aResponse(Oferta oferta, Long usuarioId) {
        ParteOferta miRol = oferta.parteDe(usuarioId);
        Usuario otraParte = miRol == ParteOferta.COMPRADOR ? oferta.getVendedor() : oferta.getComprador();
        var contraparte = new OfertaDtos.Contraparte(otraParte.getId(), perfiles.nombrePublico(otraParte));
        return OfertaDtos.Response.from(oferta, miRol, contraparte);
    }

    /** Devuelve 404 tanto si no existe como si el usuario no participa: no revela ofertas ajenas. */
    private Oferta buscarComoParticipante(Long ofertaId, Usuario usuario) {
        return ofertas.findById(ofertaId)
                .filter(oferta -> oferta.parteDe(usuario.getId()) != null)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Oferta no encontrada"));
    }

    private void validarQuePuedeResponder(Oferta oferta, Usuario usuario) {
        if (oferta.getEstado() != EstadoOferta.PENDIENTE) {
            throw error(HttpStatus.CONFLICT, "La oferta ya está " + oferta.getEstado().descripcion());
        }
        if (!oferta.puedeResponder(oferta.parteDe(usuario.getId()))) {
            throw error(HttpStatus.CONFLICT, "Tenés que esperar la respuesta de la otra parte");
        }
    }

    private Usuario usuarioPorEmail(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }

    private static ResponseStatusException error(HttpStatus status, String mensaje) {
        return new ResponseStatusException(status, mensaje);
    }

    private static Instant ahora() {
        return Instant.now();
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
