package com.ronda.backend.favorito;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.publicacion.EstadoArticulo;
import com.ronda.backend.publicacion.ExplorarFiltros;
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
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "busquedas_guardadas", schema = "ronda")
public class BusquedaGuardada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 120)
    private String texto;
    @Column(length = 80)
    private String categoria;
    @Column(name = "precio_min", precision = 12, scale = 2)
    private BigDecimal precioMin;
    @Column(name = "precio_max", precision = 12, scale = 2)
    private BigDecimal precioMax;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_articulo", length = 20)
    private EstadoArticulo estadoArticulo;
    @Column(precision = 9, scale = 6)
    private BigDecimal latitud;
    @Column(precision = 9, scale = 6)
    private BigDecimal longitud;
    @Column(name = "radio_km", precision = 6, scale = 2)
    private BigDecimal radioKm;

    @Column(name = "ultima_revision_at", nullable = false)
    private Instant ultimaRevisionAt = Instant.now();
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected BusquedaGuardada() { }

    public BusquedaGuardada(Usuario usuario, String nombre, ExplorarFiltros filtros) {
        this.usuario = usuario;
        this.nombre = nombre;
        aplicarFiltros(filtros);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public Instant getUltimaRevisionAt() { return ultimaRevisionAt; }
    public Instant getCreatedAt() { return createdAt; }

    /** true si esta búsqueda filtra por cercanía (el radio solo se usa cuando hay coordenadas). */
    public boolean isCercania() {
        return latitud != null && longitud != null;
    }

    /** Reconstruye los filtros originales para volver a ejecutar la búsqueda. */
    public ExplorarFiltros aFiltros() {
        return new ExplorarFiltros(texto, categoria, precioMin, precioMax, estadoArticulo,
                latitud == null ? null : latitud.doubleValue(),
                longitud == null ? null : longitud.doubleValue(),
                radioKm == null ? null : radioKm.doubleValue());
    }

    /** Limpia el indicador de novedades: desde ahora solo cuentan publicaciones futuras. */
    public void marcarVista() {
        this.ultimaRevisionAt = Instant.now();
    }

    /**
     * Actualiza el nombre y los filtros de una búsqueda ya guardada, sin crear una nueva.
     * Reinicia el indicador de novedades: al cambiar los filtros, el conjunto de resultados
     * cambia y ya no tiene sentido compararlo contra la última revisión de los filtros viejos.
     */
    public void actualizar(String nombre, ExplorarFiltros filtros) {
        this.nombre = nombre;
        aplicarFiltros(filtros);
        marcarVista();
    }

    private void aplicarFiltros(ExplorarFiltros filtros) {
        this.texto = filtros.q();
        this.categoria = filtros.categoria();
        this.precioMin = filtros.precioMin();
        this.precioMax = filtros.precioMax();
        this.estadoArticulo = filtros.estadoArticulo();
        this.latitud = filtros.lat() == null ? null : BigDecimal.valueOf(filtros.lat());
        this.longitud = filtros.lng() == null ? null : BigDecimal.valueOf(filtros.lng());
        this.radioKm = filtros.radioKm() == null ? null : BigDecimal.valueOf(filtros.radioKm());
    }
}
