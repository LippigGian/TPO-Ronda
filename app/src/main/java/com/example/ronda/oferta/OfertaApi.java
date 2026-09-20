package com.example.ronda.oferta;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Endpoints del punto 7. Se obtiene con ApiClient.crearServicio(OfertaApi.class). */
public interface OfertaApi {

    @POST("api/v1/publicaciones/{publicacionId}/ofertas")
    Call<OfertaModels.Oferta> ofertar(@Path("publicacionId") long publicacionId,
                                      @Body OfertaModels.PropuestaRequest request);

    @GET("api/v1/ofertas/mias")
    Call<OfertaModels.MisOfertas> misOfertas();

    @GET("api/v1/ofertas/{id}")
    Call<OfertaModels.Oferta> obtener(@Path("id") long ofertaId);

    @POST("api/v1/ofertas/{id}/aceptar")
    Call<OfertaModels.Oferta> aceptar(@Path("id") long ofertaId);

    @POST("api/v1/ofertas/{id}/rechazar")
    Call<OfertaModels.Oferta> rechazar(@Path("id") long ofertaId);

    @POST("api/v1/ofertas/{id}/contraofertar")
    Call<OfertaModels.Oferta> contraofertar(@Path("id") long ofertaId,
                                            @Body OfertaModels.PropuestaRequest request);
}
