package com.example.ronda;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;


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
    @GET("api/v1/publicaciones/mias")
    Call<List<ApiClient.PublicacionResponse>> getMisPublicaciones();

    @PATCH("api/v1/publicaciones/{id}/estado")
    Call<ApiClient.PublicacionResponse> changePublicacionStatus(
            @Path("id") long id,
            @Body ApiClient.ChangePublicacionStatusRequest request
    );
}
