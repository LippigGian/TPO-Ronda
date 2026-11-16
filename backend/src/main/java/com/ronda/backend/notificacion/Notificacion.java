package com.ronda.backend.notificacion;

import com.ronda.backend.auth.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Aviso para un usuario (punto 11). La crea NotificacionService en la misma transaccion del evento. */
@Entity
@Table(name = "notificaciones", schema = "ronda")
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 300)
    private String mensaje;

    @Column(name = "publicacion_id")
    private Long publicacionId;

    @Column(name = "oferta_id")
    private Long ofertaId;

    @Column(nullable = false)
    private boolean entregada = false;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Notificacion() { }

    public Notificacion(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje,
                        Long publicacionId, Long ofertaId) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.titulo = recortar(titulo, 120);
        this.mensaje = recortar(mensaje, 300);
        this.publicacionId = publicacionId;
        this.ofertaId = ofertaId;
    }

    public void marcarEntregada() { this.entregada = true; }
    public void marcarLeida() { this.leida = true; }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public TipoNotificacion getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public Long getPublicacionId() { return publicacionId; }
    public Long getOfertaId() { return ofertaId; }
    public boolean isEntregada() { return entregada; }
    public boolean isLeida() { return leida; }
    public Instant getCreatedAt() { return createdAt; }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }
}
