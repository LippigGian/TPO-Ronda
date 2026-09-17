package com.ronda.backend.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.UserResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return AuthDtos.UserResponse.from(authService.register(request));
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/otp/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthDtos.OtpRequestResponse requestOtp(@Valid @RequestBody AuthDtos.OtpRequest request) {
        return otpService.requestCode(request);
    }

    @PostMapping("/otp/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthDtos.OtpRequestResponse resendOtp(@Valid @RequestBody AuthDtos.OtpRequest request) {
        return otpService.requestCode(request);
    }

    @PostMapping("/otp/verify")
    public AuthDtos.LoginResponse verifyOtp(@Valid @RequestBody AuthDtos.OtpVerifyRequest request) {
        return otpService.verifyCode(request);
    }
}
