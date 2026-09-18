package com.ronda.backend.publicacion;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "publicacion_fotos", schema = "ronda")
public class PublicacionFoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;
    @Column(nullable = false, unique = true, length = 255)
    private String archivo;
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
    @Column(nullable = false)
    private int orden;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PublicacionFoto() { }

    public PublicacionFoto(Publicacion publicacion, String archivo, String contentType, int orden) {
        this.publicacion = publicacion;
        this.archivo = archivo;
        this.contentType = contentType;
        this.orden = orden;
    }

    public String getArchivo() { return archivo; }
}
