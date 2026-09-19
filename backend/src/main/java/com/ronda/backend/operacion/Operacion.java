package com.ronda.backend.operacion;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.publicacion.Publicacion;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "operaciones", schema = "ronda")
public class Operacion {
    public static final Duration VENTANA_CALIFICACION = Duration.ofDays(7);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comprador_id", nullable = false)
    private Usuario comprador;

    @Column(name = "monto_final", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoFinal;

    @Column(name = "fecha_operacion", nullable = false)
    private Instant fechaOperacion = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Operacion() { }

    public Operacion(Publicacion publicacion, Usuario vendedor, Usuario comprador, BigDecimal montoFinal) {
        this.publicacion = publicacion;
        this.vendedor = vendedor;
        this.comprador = comprador;
        this.montoFinal = montoFinal;
    }

    public Long getId() { return id; }
    public Publicacion getPublicacion() { return publicacion; }
    public Usuario getVendedor() { return vendedor; }
    public Usuario getComprador() { return comprador; }
    public BigDecimal getMontoFinal() { return montoFinal; }
    public Instant getFechaOperacion() { return fechaOperacion; }

    public boolean esParte(Long usuarioId) {
        return vendedor.getId().equals(usuarioId) || comprador.getId().equals(usuarioId);
    }

    public Usuario contraparte(Long usuarioId) {
        return vendedor.getId().equals(usuarioId) ? comprador : vendedor;
    }

    public boolean puedeCalificar(Instant ahora) {
        return ahora.isBefore(fechaOperacion.plus(VENTANA_CALIFICACION));
    }
}
