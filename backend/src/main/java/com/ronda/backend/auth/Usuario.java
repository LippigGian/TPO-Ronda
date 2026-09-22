package com.ronda.backend.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usuarios", schema = "ronda")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(name = "nombre_usuario", nullable = false, length = 80)
    private String nombreUsuario;
    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = false;
    @Column(nullable = false)
    private boolean activo = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Usuario() { }

    public Usuario(String email, String passwordHash, String nombreUsuario) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombreUsuario = nombreUsuario;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getNombreUsuario() { return nombreUsuario; }
    public boolean isEmailVerificado() { return emailVerificado; }
    public boolean isActivo() { return activo; }
    public Instant getCreatedAt() { return createdAt; }
    public void verificarEmail() { this.emailVerificado = true; }
    public void cambiarEmail(String nuevoEmail) { this.email = nuevoEmail; }
}
