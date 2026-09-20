package com.example.ronda;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.DELETE;

import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.Query;


/**     INTERFAZ DONDE SOLO SE DECLARAN LOS METODOS HTTP **/
public interface RondaApi {

    /** Metodos de autenticacion **/
    @POST("api/v1/auth/login")
    Call<ApiClient.LoginResponse> login(@Body ApiClient.LoginRequest request);

    @POST("api/v1/auth/register")
    Call<Void> register(@Body ApiClient.RegisterRequest request);

    @POST("api/v1/auth/otp/request")
    Call<ApiClient.OtpRequestResponse> requestOtp(@Body ApiClient.OtpRequest request);

    @POST("api/v1/auth/otp/resend")
    Call<ApiClient.OtpRequestResponse> resendOtp(@Body ApiClient.OtpRequest request);

    @POST("api/v1/auth/otp/verify")
    Call<ApiClient.LoginResponse> verifyOtp(@Body ApiClient.OtpVerifyRequest request);

    @POST("api/v1/publicaciones")
    Call<ApiClient.PublicacionResponse> createPublicacion(
            @Body ApiClient.CreatePublicacionRequest request
    );

    /** Publicaciones **/
    /** Home: listado paginado de publicaciones activas. Los filtros nulos no se envían. **/
    @GET("api/v1/publicaciones")
    Call<ApiClient.PageResponse<ApiClient.PublicacionResponse>> explorarPublicaciones(
            @Query("q") String texto,
            @Query("categoria") String categoria,
            @Query("precioMin") Double precioMin,
            @Query("precioMax") Double precioMax,
            @Query("estadoArticulo") String estadoArticulo,
            @Query("lat") Double latitud,
            @Query("lng") Double longitud,
            @Query("radioKm") Double radioKm,
            @Query("orden") String orden,
            @Query("page") int pagina,
            @Query("size") int tamano
    );

    @GET("api/v1/publicaciones/categorias")
    Call<List<String>> getCategorias();

    @GET("api/v1/publicaciones/mias")
    Call<List<ApiClient.PublicacionResponse>> getMisPublicaciones();

    @PATCH("api/v1/publicaciones/{id}/estado")
    Call<ApiClient.PublicacionResponse> changePublicacionStatus(
            @Path("id") long id,
            @Body ApiClient.ChangePublicacionStatusRequest request
    );
    /** Multipart indica a retrofit que se enviaran archivos, no solo JSON**/
    @Multipart
    @POST("api/v1/publicaciones/{id}/fotos")
    Call<ApiClient.PublicacionResponse> uploadFotos(
            @Path("id") long id,
            @Part List<MultipartBody.Part> fotos
    );

    @DELETE("api/v1/publicaciones/{id}")
    Call<Void> deletePublicacion(@Path("id") long id);

}
