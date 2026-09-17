package com.example.ronda;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;


/**     INTERFAZ DONDE SOLO SE DECLARAN LOS METODOS HTTP **/
public interface RondaApi {
    @POST("api/v1/auth/login")
    Call<ApiClient.LoginResponse> login(@Body ApiClient.LoginRequest request);

    @POST("api/v1/auth/otp/request")
    Call<ApiClient.OtpRequestResponse> requestOtp(@Body ApiClient.OtpRequest request);

    @POST("api/v1/auth/otp/resend")
    Call<ApiClient.OtpRequestResponse> resendOtp(@Body ApiClient.OtpRequest request);

    @POST("api/v1/auth/otp/verify")
    Call<ApiClient.LoginResponse> verifyOtp(@Body ApiClient.OtpVerifyRequest request);
}
