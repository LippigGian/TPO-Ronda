package com.ronda.backend.publicacion;

import com.ronda.backend.auth.Usuario;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publicaciones", schema = "ronda")
public class Publicacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<PublicacionFoto> fotos = new ArrayList<>();

    @Column(nullable = false, length = 120)
    private String titulo;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    @Column(nullable = false, length = 80)
    private String categoria;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_articulo", nullable = false, length = 20)
    private EstadoArticulo estadoArticulo;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_publicacion", nullable = false, length = 20)
    private EstadoPublicacion estadoPublicacion = EstadoPublicacion.ACTIVA;
    @Column(nullable = false, length = 255)
    private String direccion;
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Publicacion() { }

    public Publicacion(Usuario vendedor, PublicacionDtos.CreateRequest request) {
        this.vendedor = vendedor;
        this.titulo = request.titulo().trim();
        this.descripcion = request.descripcion().trim();
        this.categoria = request.categoria().trim();
        this.precio = request.precio();
        this.estadoArticulo = request.estadoArticulo();
        this.direccion = request.direccion().trim();
        this.latitud = request.latitud();
        this.longitud = request.longitud();
    }

    public Long getId() { return id; }
    public Usuario getVendedor() { return vendedor; }
    public List<PublicacionFoto> getFotos() { return fotos; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPrecio() { return precio; }
    public EstadoArticulo getEstadoArticulo() { return estadoArticulo; }
    public EstadoPublicacion getEstadoPublicacion() { return estadoPublicacion; }
    public String getDireccion() { return direccion; }
    public BigDecimal getLatitud() { return latitud; }
    public BigDecimal getLongitud() { return longitud; }
    public Instant getCreatedAt() { return createdAt; }

    public void cambiarEstado(EstadoPublicacion estado) {
        this.estadoPublicacion = estado;
        this.updatedAt = Instant.now();
    }
}
