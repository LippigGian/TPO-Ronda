package com.ronda.backend.favorito;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.publicacion.Publicacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "favoritos", schema = "ronda")
public class Favorito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    /** Precio de la publicación al momento de agregarla (o de la última vez que se vio); permite detectar cambios. */
    @Column(name = "precio_guardado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioGuardado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Favorito() { }

    public Favorito(Usuario usuario, Publicacion publicacion) {
        this.usuario = usuario;
        this.publicacion = publicacion;
        this.precioGuardado = publicacion.getPrecio();
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Publicacion getPublicacion() { return publicacion; }
    public BigDecimal getPrecioGuardado() { return precioGuardado; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean cambioPrecio() {
        return precioGuardado.compareTo(publicacion.getPrecio()) != 0;
    }

    /** Actualiza el snapshot al precio actual; se usa al abrir el detalle para limpiar el indicador. */
    public void marcarVisto() {
        this.precioGuardado = publicacion.getPrecio();
    }
}
