package com.ronda.backend.oferta;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.publicacion.Publicacion;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Negociacion de precio entre un comprador y el vendedor de una publicacion.
 *
 * - precio: ultima propuesta (la inicial del comprador o la ultima contraoferta).
 * - turno: quien tiene que responder. Quien tiene el turno puede aceptar, rechazar o contraofertar.
 * - venceAt: plazo de vigencia; se renueva con cada contraoferta.
 *
 * La entidad solo cambia su propio estado; las reglas que dependen de otros datos
 * (permisos, estado de la publicacion) viven en OfertaService.
 */
@Entity
@Table(name = "ofertas", schema = "ronda")
public class Oferta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comprador_id", nullable = false)
    private Usuario comprador;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(length = 280)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOferta estado = EstadoOferta.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParteOferta turno = ParteOferta.VENDEDOR;

    @Column(name = "vence_at", nullable = false)
    private Instant venceAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Oferta() { }

    /** Oferta inicial del comprador: el turno arranca en el vendedor. */
    public Oferta(Publicacion publicacion, Usuario comprador, BigDecimal precio, String mensaje, Instant venceAt) {
        this.publicacion = publicacion;
        this.comprador = comprador;
        this.precio = precio;
        this.mensaje = mensaje;
        this.venceAt = venceAt;
    }

    /** Rol del usuario en esta negociacion, o null si no participa. */
    public ParteOferta parteDe(Long usuarioId) {
        if (comprador.getId().equals(usuarioId)) {
            return ParteOferta.COMPRADOR;
        }
        if (publicacion.getVendedor().getId().equals(usuarioId)) {
            return ParteOferta.VENDEDOR;
        }
        return null;
    }

    /** true si la oferta sigue abierta y le toca responder a esa parte. */
    public boolean puedeResponder(ParteOferta parte) {
        return estado == EstadoOferta.PENDIENTE && turno == parte;
    }

    public void aceptar(Instant ahora) {
        cerrar(EstadoOferta.ACEPTADA, ahora);
    }

    public void rechazar(Instant ahora) {
        cerrar(EstadoOferta.RECHAZADA, ahora);
    }

    /** Nueva propuesta de precio: le pasa el turno a la otra parte y renueva el plazo. */
    public void contraofertar(BigDecimal nuevoPrecio, String nuevoMensaje, Instant nuevoVencimiento, Instant ahora) {
        exigirPendiente();
        this.precio = nuevoPrecio;
        this.mensaje = nuevoMensaje;
        this.turno = turno.otra();
        this.venceAt = nuevoVencimiento;
        this.updatedAt = ahora;
    }

    private void cerrar(EstadoOferta estadoFinal, Instant ahora) {
        exigirPendiente();
        this.estado = estadoFinal;
        this.updatedAt = ahora;
    }

    private void exigirPendiente() {
        if (estado != EstadoOferta.PENDIENTE) {
            throw new IllegalStateException("La oferta ya no está pendiente");
        }
    }

    public Long getId() { return id; }
    public Publicacion getPublicacion() { return publicacion; }
    public Usuario getComprador() { return comprador; }
    public Usuario getVendedor() { return publicacion.getVendedor(); }
    public BigDecimal getPrecio() { return precio; }
    public String getMensaje() { return mensaje; }
    public EstadoOferta getEstado() { return estado; }
    public ParteOferta getTurno() { return turno; }
    public Instant getVenceAt() { return venceAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
