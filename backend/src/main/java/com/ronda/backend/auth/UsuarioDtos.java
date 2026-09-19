package com.ronda.backend.auth;

public final class UsuarioDtos {
    private UsuarioDtos() { }

    public record PublicResponse(Long id, String nombreUsuario) {
        static PublicResponse from(Usuario usuario) {
            return new PublicResponse(usuario.getId(), usuario.getNombreUsuario());
        }
    }
}
