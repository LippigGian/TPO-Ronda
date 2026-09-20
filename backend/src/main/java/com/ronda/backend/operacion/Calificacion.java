package com.ronda.backend.operacion;

import com.ronda.backend.auth.Usuario;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "calificaciones", schema = "ronda")
public class Calificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacion_id", nullable = false)
    private Operacion operacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receptor_id", nullable = false)
    private Usuario receptor;

    @Column(nullable = false)
    private int puntaje;

    @Column(length = 500)
    private String comentario;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Calificacion() { }

    public Calificacion(Operacion operacion, Usuario autor, Usuario receptor, int puntaje, String comentario) {
        this.operacion = operacion;
        this.autor = autor;
        this.receptor = receptor;
        this.puntaje = puntaje;
        this.comentario = comentario;
    }

    public Long getId() { return id; }
    public Operacion getOperacion() { return operacion; }
    public Usuario getAutor() { return autor; }
    public Usuario getReceptor() { return receptor; }
    public int getPuntaje() { return puntaje; }
    public String getComentario() { return comentario; }
    public Instant getCreatedAt() { return createdAt; }
}
