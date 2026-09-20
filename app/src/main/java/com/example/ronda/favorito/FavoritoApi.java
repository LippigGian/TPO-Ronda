package com.example.ronda.favorito;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/** Endpoints del punto 10. Se obtiene con ApiClient.crearServicio(FavoritoApi.class). */
public interface FavoritoApi {

    @POST("api/v1/publicaciones/{id}/favorito")
    Call<Void> marcar(@Path("id") long publicacionId);

    @DELETE("api/v1/publicaciones/{id}/favorito")
    Call<Void> desmarcar(@Path("id") long publicacionId);

    @GET("api/v1/favoritos")
    Call<List<FavoritoModels.FavoritoItem>> listarFavoritos();

    @GET("api/v1/favoritos/ids")
    Call<List<Long>> listarIdsFavoritos();

    @POST("api/v1/favoritos/{publicacionId}/visto")
    Call<Void> marcarVisto(@Path("publicacionId") long publicacionId);

    @POST("api/v1/busquedas-guardadas")
    Call<FavoritoModels.BusquedaGuardadaItem> guardarBusqueda(@Body FavoritoModels.GuardarBusquedaRequest request);

    @PUT("api/v1/busquedas-guardadas/{id}")
    Call<FavoritoModels.BusquedaGuardadaItem> actualizarBusqueda(@Path("id") long id,
            @Body FavoritoModels.GuardarBusquedaRequest request);

    @GET("api/v1/busquedas-guardadas")
    Call<List<FavoritoModels.BusquedaGuardadaItem>> listarBusquedas();

    @POST("api/v1/busquedas-guardadas/{id}/marcar-vista")
    Call<Void> marcarBusquedaVista(@Path("id") long id);

    @DELETE("api/v1/busquedas-guardadas/{id}")
    Call<Void> eliminarBusqueda(@Path("id") long id);
}
