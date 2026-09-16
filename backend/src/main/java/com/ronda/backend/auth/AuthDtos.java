package com.ronda.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() { }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(min = 2, max = 80) String username) { }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank String password) { }

    public record OtpRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotNull OtpPurpose purpose) { }

    public record OtpVerifyRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotNull OtpPurpose purpose,
            @NotBlank @Pattern(regexp = "\\d{6}") String code) { }

    public record OtpRequestResponse(String message, long expiresInSeconds) { }

    public record UserResponse(Long id, String email, String username) {
        static UserResponse from(Usuario user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getNombreUsuario());
        }
    }

    public record LoginResponse(String token, String tokenType, long expiresIn, UserResponse user) { }
}
