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
        private UserResponse user;

        public String getToken() { return token; }
        public String getTokenType() { return tokenType; }
        public long getExpiresIn() { return expiresIn; }
        public UserResponse getUser() { return user; }
    }

    public static final class UserResponse {
        private long id;
        private String email;
        private String username;

        public long getId() { return id; }
        public String getEmail() { return email; }
        public String getUsername() { return username; }
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
        // Campos que completa el listado público (Home): zona aproximada y distancia al usuario.
        private String zona;
        private String createdAt;
        private Integer distanciaKm;
        // Campos que completa el detalle: rol de quien mira, permiso sobre la dirección y datos del vendedor.
        private boolean esPropia;
        private boolean direccionVisible;
        private VendedorResponse vendedor;

        public boolean isEsPropia() {
            return esPropia;
        }

        /** false mientras el usuario no pueda ver la dirección exacta (antes de una oferta aceptada). */
        public boolean isDireccionVisible() {
            return direccionVisible;
        }

        public VendedorResponse getVendedor() {
            return vendedor;
        }

        public String getZona() {
            return zona;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        /** Distancia aproximada en km al usuario; null si el listado no se pidió con ubicación. */
        public Integer getDistanciaKm() {
            return distanciaKm;
        }

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

    /** Página de resultados del backend (content + datos de paginación). */
    public static final class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;

        public List<T> getContent() { return content; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
        public boolean isLast() { return last; }
    }

    public static final class VendedorResponse {
        private long id;
        private String nombre;
        private String miembroDesde;
        private ReputacionResponse reputacion;

        public long getId() { return id; }
        public String getNombre() { return nombre; }
        public String getMiembroDesde() { return miembroDesde; }
        public ReputacionResponse getReputacion() { return reputacion; }
    }

    public static final class ReputacionResponse {
        private Double promedioEstrellas;
        private int cantidadCalificaciones;
        private int operacionesComoVendedor;
        private int operacionesComoComprador;

        public Double getPromedioEstrellas() { return promedioEstrellas; }
        public int getCantidadCalificaciones() { return cantidadCalificaciones; }
        public int getOperacionesComoVendedor() { return operacionesComoVendedor; }
        public int getOperacionesComoComprador() { return operacionesComoComprador; }
    }
    /** Búsqueda de usuario por email (para elegir comprador al vender) **/
    public static final class BuscarUsuarioResponse {
        private long id;
        private String nombreUsuario;

        public long getId() { return id; }
        public String getNombreUsuario() { return nombreUsuario; }
    }

    /** Venta de una publicación (crea la operación) **/
    public static final class VenderRequest {
        private final String compradorEmail;
        private final double montoFinal;

        public VenderRequest(String compradorEmail, double montoFinal) {
            this.compradorEmail = compradorEmail;
            this.montoFinal = montoFinal;
        }
    }

    /** Historial de operaciones (compras/ventas) **/
    public static final class HistorialItemResponse {
        private long id;
        private String tipo;
        private long publicacionId;
        private String publicacionTitulo;
        private double montoFinal;
        private String fechaOperacion;
        private long contraparteId;
        private String contraparteNombre;
        private boolean yaCalificada;
        private String puedeCalificarHasta;

        public long getId() { return id; }
        public String getTipo() { return tipo; }
        public long getPublicacionId() { return publicacionId; }
        public String getPublicacionTitulo() { return publicacionTitulo; }
        public double getMontoFinal() { return montoFinal; }
        public String getFechaOperacion() { return fechaOperacion; }
        public long getContraparteId() { return contraparteId; }
        public String getContraparteNombre() { return contraparteNombre; }
        public boolean isYaCalificada() { return yaCalificada; }
        public String getPuedeCalificarHasta() { return puedeCalificarHasta; }
        public boolean puedeCalificarAhora() {
            return !yaCalificada && puedeCalificarHasta != null;
        }
    }

    /** Calificaciones **/
    public static final class CalificacionRequest {
        private final int puntaje;
        private final String comentario;

        public CalificacionRequest(int puntaje, String comentario) {
            this.puntaje = puntaje;
            this.comentario = comentario;
        }
    }

    public static final class CalificacionResponse {
        private long id;
        private long operacionId;
        private String autorNombre;
        private String receptorNombre;
        private int puntaje;
        private String comentario;
        private String createdAt;

        public long getId() { return id; }
        public String getAutorNombre() { return autorNombre; }
        public String getReceptorNombre() { return receptorNombre; }
        public int getPuntaje() { return puntaje; }
        public String getComentario() { return comentario; }
        public String getCreatedAt() { return createdAt; }
    }

   


}
