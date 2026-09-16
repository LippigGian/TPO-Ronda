package com.ronda.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                       JwtService jwtService, OtpService otpService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
    }

    @Transactional
    public Usuario register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya esta registrado");
        }
        Usuario user = usuarios.save(new Usuario(email, passwordEncoder.encode(request.password()), request.username().trim()));
        otpService.sendCode(user, OtpPurpose.REGISTRO);
        return user;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Usuario user = usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }
        if (!user.isEmailVerificado() || !user.isActivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no esta habilitada");
        }
        return new AuthDtos.LoginResponse(jwtService.createToken(user), "Bearer",
                jwtService.getExpirationSeconds(), AuthDtos.UserResponse.from(user));
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Usuario user = usuarios.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(user.getEmail()).password(user.getPasswordHash())
                .disabled(!user.isActivo()).authorities("USER").build();
    }
}
