package com.ronda.backend.perfil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Datos personales editables de un usuario.
 * Comparte la clave primaria con la tabla usuarios (relacion 1 a 1).
 * Si un usuario nunca edito su perfil, la fila todavia no existe.
 */
@Entity
@Table(name = "perfiles", schema = "ronda")
public class Perfil {
    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 120)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Column(length = 120)
    private String zona;

    @Column(name = "foto_archivo", length = 255)
    private String fotoArchivo;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Perfil() { }

    public Perfil(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public void actualizarDatos(String nombre, String telefono, String zona) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.zona = zona;
        this.updatedAt = Instant.now();
    }

    /** Reemplaza la foto y devuelve el nombre del archivo anterior (o null) para poder borrarlo. */
    public String cambiarFoto(String nuevoArchivo) {
        String anterior = this.fotoArchivo;
        this.fotoArchivo = nuevoArchivo;
        this.updatedAt = Instant.now();
        return anterior;
    }

    /** URL relativa que sirve UploadResourceConfig, igual que las fotos de publicaciones. */
    public String getFotoUrl() {
        return fotoArchivo == null ? null : "/uploads/" + fotoArchivo;
    }

    public Long getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getZona() { return zona; }
}
