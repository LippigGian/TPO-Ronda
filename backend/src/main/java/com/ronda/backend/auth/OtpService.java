package com.ronda.backend.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OtpService {
    private static final int MAX_ATTEMPTS = 5;
    private final OtpCodeRepository codes;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();
    private final long expirationSeconds;
    private final String sender;

    public OtpService(OtpCodeRepository codes, UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                      JavaMailSender mailSender, JwtService jwtService,
                      @Value("${app.otp.expiration-seconds:600}") long expirationSeconds,
                      @Value("${app.otp.sender}") String sender) {
        this.codes = codes;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.jwtService = jwtService;
        this.expirationSeconds = expirationSeconds;
        this.sender = sender;
    }

    @Transactional
    public AuthDtos.OtpRequestResponse requestCode(AuthDtos.OtpRequest request) {
        String email = normalizeEmail(request.email());
        Usuario user = findUser(email);

        if (request.purpose() == OtpPurpose.REGISTRO && user.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya fue verificada");
        }
        if (request.purpose() != OtpPurpose.REGISTRO && !user.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Primero verificá tu email");
        }
        if (!user.isActivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no está habilitada");
        }
        return sendCode(user, request.purpose());
    }

    @Transactional
    public AuthDtos.OtpRequestResponse sendCode(Usuario user, OtpPurpose purpose) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        List<OtpCode> pendingCodes = codes.findByEmailAndPurposeAndUsedAtIsNull(user.getEmail(), purpose);
        codes.deleteAll(pendingCodes);
        codes.save(new OtpCode(user.getEmail(), purpose, passwordEncoder.encode(code),
                Instant.now().plusSeconds(expirationSeconds)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(user.getEmail());
        message.setSubject("Código de acceso Ronda");
        message.setText("Tu código es: " + code + "\n\nVence en " + (expirationSeconds / 60)
                + " minutos. Si no solicitaste este código, ignorá este correo.");
        mailSender.send(message);

        return new AuthDtos.OtpRequestResponse("Código enviado", expirationSeconds);
    }

    @Transactional
    public AuthDtos.LoginResponse verifyCode(AuthDtos.OtpVerifyRequest request) {
        String email = normalizeEmail(request.email());
        OtpCode otp = codes.findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(email, request.purpose())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay un código pendiente"));

        if (otp.isExpired(Instant.now())) {
            codes.delete(otp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código venció. Solicitá uno nuevo");
        }
        if (!passwordEncoder.matches(request.code(), otp.getCodeHash())) {
            otp.registerFailedAttempt();
            if (otp.getAttempts() >= MAX_ATTEMPTS) {
                codes.delete(otp);
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido");
        }

        Usuario user = findUser(email);
        if (!user.isActivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no está habilitada");
        }
        if (request.purpose() == OtpPurpose.REGISTRO) {
            user.verificarEmail();
        } else if (!user.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Primero verificá tu email");
        }
        otp.markAsUsed();
        return new AuthDtos.LoginResponse(jwtService.createToken(user), "Bearer",
                jwtService.getExpirationSeconds(), AuthDtos.UserResponse.from(user));
    }

    private Usuario findUser(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una cuenta con ese email"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
