package com.example.ronda;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import java.util.List;


public final class ApiClient {
    private static final String BASE_URL = "http://127.0.0.1:8080/";

    /** Preparo Retrofti para utilizar la ainterfaz RondaApi **/
    private static RondaApi api;
    private static Retrofit retrofit;
    private static SessionManager sessionManager;

    private ApiClient() { }

    public static void initialize(Context context) {
        if (api != null) {
            return;
        }

        sessionManager = new SessionManager(context);
/** Interceptor para agregar el token a cada request que pase por ApiClient **/
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    String token = sessionManager.getToken();

                    if (token == null) {
                        return chain.proceed(request);
                    }

                    Request authenticatedRequest = request.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .build();

                    return chain.proceed(authenticatedRequest);
                })
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(RondaApi.class);
    }

    /**
     * Crea la implementacion de cualquier interfaz Retrofit (ej: PerfilApi.class)
     * reutilizando la misma configuracion: URL base, token JWT y Gson.
     * Permite que cada feature tenga su propia interfaz sin tocar RondaApi.
     */
    public static <T> T crearServicio(Class<T> servicio) {
        if (retrofit == null) {
            throw new IllegalStateException("ApiClient no fue inicializado");
        }

        return retrofit.create(servicio);
    }

    public static RondaApi api() {
        if (api == null) {
            throw new IllegalStateException("ApiClient no fue inicializado");
        }

        return api;
    }

    public static String imageUrl(String path) {
        return BASE_URL + path.replaceFirst("^/", "");
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
    public static final class CreatePublicacionRequest {
        private final String titulo;
        private final String descripcion;
        private final String categoria;
        private final double precio;
        private final String estadoArticulo;
        private final String direccion;
        private final double latitud;
        private final double longitud;

        public CreatePublicacionRequest(
                String titulo,
                String descripcion,
                String categoria,
                double precio,
                String estadoArticulo,
                String direccion,
                double latitud,
                double longitud
        ) {
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.categoria = categoria;
            this.precio = precio;
            this.estadoArticulo = estadoArticulo;
            this.direccion = direccion;
            this.latitud = latitud;
            this.longitud = longitud;
        }
    }

    public static final class ChangePublicacionStatusRequest {
        private final String estado;

        public ChangePublicacionStatusRequest(String estado) {
            this.estado = estado;
        }
    }

    public static final class PublicacionResponse {
        private long id;
        private String titulo;
        private String descripcion;
        private String categoria;
        private double precio;
        private String estadoArticulo;
        private String estadoPublicacion;
        private String direccion;
        private double latitud;
        private double longitud;
        private List<String> fotos;

        public long getId() {
            return id;
        }

        public String getTitulo() {
            return titulo;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public String getCategoria() {
            return categoria;
        }

        public double getPrecio() {
            return precio;
        }

        public String getEstadoArticulo() {
            return estadoArticulo;
        }

        public String getEstadoPublicacion() {
            return estadoPublicacion;
        }

        public String getDireccion() {
            return direccion;
        }

        public double getLatitud() {
            return latitud;
        }

        public double getLongitud() {
            return longitud;
        }

        public List<String> getFotos() {
            return fotos;
        }
    }

}
