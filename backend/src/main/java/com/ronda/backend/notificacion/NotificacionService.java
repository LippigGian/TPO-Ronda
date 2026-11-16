package com.ronda.backend.notificacion;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Punto 11: avisos de ofertas y preguntas.
 *
 * Los metodos "notificar*" los llaman los servicios de cada feature dentro de su transaccion
 * (MANDATORY): si la accion falla y se revierte, el aviso tampoco se guarda.
 * La app consulta /notificaciones/nuevas y muestra cada aviso como notificacion del sistema.
 */
@Service
public class NotificacionService {
    private final NotificacionRepository notificaciones;
    private final UsuarioRepository usuarios;

    public NotificacionService(NotificacionRepository notificaciones, UsuarioRepository usuarios) {
        this.notificaciones = notificaciones;
        this.usuarios = usuarios;
    }

    // ---------- Eventos de ofertas (punto 7) ----------

    @Transactional(propagation = Propagation.MANDATORY)
    public void ofertaRecibida(Usuario vendedor, String comprador, String titulo, BigDecimal precio,
                               Long publicacionId, Long ofertaId) {
        guardar(vendedor, TipoNotificacion.OFERTA_RECIBIDA, "Nueva oferta",
                comprador + " ofreció " + precio(precio) + " por \"" + titulo + "\"", publicacionId, ofertaId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void contraofertaRecibida(Usuario destinatario, String autor, String titulo, BigDecimal precio,
                                     Long publicacionId, Long ofertaId) {
        guardar(destinatario, TipoNotificacion.CONTRAOFERTA_RECIBIDA, "Contraoferta recibida",
                autor + " propuso " + precio(precio) + " por \"" + titulo + "\"", publicacionId, ofertaId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void ofertaAceptada(Usuario destinatario, String autor, String titulo, BigDecimal precio,
                               Long publicacionId, Long ofertaId) {
        guardar(destinatario, TipoNotificacion.OFERTA_ACEPTADA, "¡Oferta aceptada!",
                autor + " aceptó " + precio(precio) + " por \"" + titulo + "\". Ya podés ver el punto de entrega.",
                publicacionId, ofertaId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void ofertaRechazada(Usuario destinatario, String autor, String titulo, Long publicacionId,
                                Long ofertaId) {
        guardar(destinatario, TipoNotificacion.OFERTA_RECHAZADA, "Oferta rechazada",
                autor + " rechazó la oferta por \"" + titulo + "\"", publicacionId, ofertaId);
    }

    /** Ofertas cerradas automaticamente porque el vendedor acepto la de otra persona. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void ofertaRechazadaPorOtraAceptada(Usuario comprador, String titulo, Long publicacionId,
                                               Long ofertaId) {
        guardar(comprador, TipoNotificacion.OFERTA_RECHAZADA, "Oferta rechazada",
                "El vendedor aceptó otra oferta por \"" + titulo + "\"", publicacionId, ofertaId);
    }

    // ---------- Eventos de preguntas (punto 4) ----------
    // Listos para cuando se implementen las preguntas: llamarlos al guardar la pregunta y la respuesta.

    @Transactional(propagation = Propagation.MANDATORY)
    public void preguntaRecibida(Usuario vendedor, String autor, String titulo, String pregunta,
                                 Long publicacionId) {
        guardar(vendedor, TipoNotificacion.PREGUNTA_RECIBIDA, "Nueva pregunta en \"" + titulo + "\"",
                autor + ": " + pregunta, publicacionId, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void preguntaRespondida(Usuario interesado, String titulo, String respuesta, Long publicacionId) {
        guardar(interesado, TipoNotificacion.PREGUNTA_RESPONDIDA, "Respondieron tu pregunta",
                "\"" + titulo + "\": " + respuesta, publicacionId, null);
    }

    // ---------- Consultas de la app ----------

    /** Avisos que la app todavia no mostro como notificacion. Quedan marcados como entregados. */
    @Transactional
    public List<NotificacionDtos.Response> nuevas(String email) {
        Usuario usuario = usuarioPorEmail(email);
        List<Notificacion> pendientes = notificaciones.findByUsuarioIdAndEntregadaFalseOrderByIdAsc(usuario.getId());
        pendientes.forEach(Notificacion::marcarEntregada);
        return pendientes.stream().map(NotificacionDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public NotificacionDtos.BandejaResponse bandeja(String email) {
        Usuario usuario = usuarioPorEmail(email);
        List<NotificacionDtos.Response> ultimas = notificaciones.findTop50ByUsuarioIdOrderByIdDesc(usuario.getId())
                .stream().map(NotificacionDtos.Response::from).toList();
        return new NotificacionDtos.BandejaResponse(ultimas, notificaciones.countByUsuarioIdAndLeidaFalse(usuario.getId()));
    }

    @Transactional
    public void marcarLeida(String email, Long id) {
        Usuario usuario = usuarioPorEmail(email);
        notificaciones.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aviso no encontrado"))
                .marcarLeida();
    }

    @Transactional
    public void marcarTodasLeidas(String email) {
        notificaciones.marcarTodasLeidas(usuarioPorEmail(email).getId());
    }

    // ---------- Helpers ----------

    private void guardar(Usuario destinatario, TipoNotificacion tipo, String titulo, String mensaje,
                         Long publicacionId, Long ofertaId) {
        notificaciones.save(new Notificacion(destinatario, tipo, titulo, mensaje, publicacionId, ofertaId));
    }

    private Usuario usuarioPorEmail(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }

    /** "$ 80.000" o "$ 80.000,50": formato argentino. */
    static String precio(BigDecimal precio) {
        DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-AR"));
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        boolean entero = precio.stripTrailingZeros().scale() <= 0;
        DecimalFormat formato = new DecimalFormat(entero ? "#,##0" : "#,##0.00", simbolos);
        return "$ " + formato.format(entero ? precio.setScale(0, RoundingMode.HALF_UP) : precio);
    }
}
