package com.example.ronda.notificacion;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/** Endpoints del punto 11. Se obtiene con ApiClient.crearServicio(NotificacionApi.class). */
public interface NotificacionApi {

    /** Avisos que todavía no se mostraron. El backend los marca como entregados al devolverlos. */
    @GET("api/v1/notificaciones/nuevas")
    Call<List<NotificacionModels.Aviso>> nuevas();
}
