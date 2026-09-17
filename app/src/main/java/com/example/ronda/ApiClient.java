package com.example.ronda;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static final String BASE_URL = "http://127.0.0.1:8080/";

    /** Preparo Retrofti para utilizar la ainterfaz RondaApi **/
    private static final RondaApi API = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RondaApi.class);

    private ApiClient() { }

    public static RondaApi api() {
        return API;
    }

    public static String errorMessage(Response<?> response, String fallback) {
        if (response.errorBody() == null) {
            return fallback;
        }

        try {
            JsonObject error = JsonParser.parseString(response.errorBody().string()).getAsJsonObject();
            if (error.has("detail")) {
                return error.get("detail").getAsString();
            }
            if (error.has("message")) {
                return error.get("message").getAsString();
            }
        } catch (IOException | IllegalStateException ignored) {
            // Si la respuesta no contiene JSON, se muestra el mensaje de respaldo.
        }
        return fallback;
    }
    /** Request para login **/
    public static final class LoginRequest {
        private final String email;
        private final String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
    /** Request para registrarse **/
    public static final class RegisterRequest {
        private final String email;
        private final String password;

        public RegisterRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static final class OtpRequest {
        private final String email;
        private final String purpose;

        public OtpRequest(String email, String purpose) {
            this.email = email;
            this.purpose = purpose;
        }
    }

    public static final class OtpVerifyRequest {
        private final String email;
        private final String purpose;
        private final String code;

        public OtpVerifyRequest(String email, String purpose, String code) {
            this.email = email;
            this.purpose = purpose;
            this.code = code;
        }
    }

    public static final class LoginResponse {
        private String token;
        private String tokenType;
        private long expiresIn;

        public String getToken() { return token; }
        public String getTokenType() { return tokenType; }
        public long getExpiresIn() { return expiresIn; }
    }

    public static final class OtpRequestResponse {
        private String message;
        private long expiresInSeconds;

        public String getMessage() { return message; }
        public long getExpiresInSeconds() { return expiresInSeconds; }
    }
}
