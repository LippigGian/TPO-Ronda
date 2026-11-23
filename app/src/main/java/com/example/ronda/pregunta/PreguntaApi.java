package com.example.ronda.pregunta;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Preguntas y respuestas (punto 4). Se obtiene con ApiClient.crearServicio(PreguntaApi.class). */
public interface PreguntaApi {

    @GET("api/v1/publicaciones/{publicacionId}/preguntas")
    Call<List<PreguntaModels.Pregunta>> listar(@Path("publicacionId") long publicacionId);

    @POST("api/v1/publicaciones/{publicacionId}/preguntas")
    Call<PreguntaModels.Pregunta> preguntar(@Path("publicacionId") long publicacionId,
                                            @Body PreguntaModels.PreguntarRequest request);

    @POST("api/v1/preguntas/{id}/respuesta")
    Call<PreguntaModels.Pregunta> responder(@Path("id") long preguntaId,
                                            @Body PreguntaModels.ResponderRequest request);
}
