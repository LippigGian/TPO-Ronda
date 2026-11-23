package com.ronda.backend.pregunta;

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
import java.time.Instant;

/** Pregunta de un interesado sobre una publicación y, si ya la contestó, la respuesta del vendedor. */
@Entity
@Table(name = "preguntas", schema = "ronda")
public class Pregunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(length = 500)
    private String respuesta;

    @Column(name = "respondida_at")
    private Instant respondidaAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Pregunta() { }

    public Pregunta(Publicacion publicacion, Usuario autor, String texto) {
        this.publicacion = publicacion;
        this.autor = autor;
        this.texto = texto;
    }

    public void responder(String respuesta) {
        this.respuesta = respuesta;
        this.respondidaAt = Instant.now();
    }

    public boolean estaRespondida() { return respuesta != null; }

    public Long getId() { return id; }
    public Publicacion getPublicacion() { return publicacion; }
    public Usuario getAutor() { return autor; }
    public String getTexto() { return texto; }
    public String getRespuesta() { return respuesta; }
    public Instant getRespondidaAt() { return respondidaAt; }
    public Instant getCreatedAt() { return createdAt; }
}
