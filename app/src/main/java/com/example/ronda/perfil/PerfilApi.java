package com.example.ronda.perfil;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Endpoints del perfil. Interfaz separada de RondaApi para que cada feature
 * tenga sus propios archivos y no haya conflictos de merge entre integrantes.
 * Se obtiene con: ApiClient.crearServicio(PerfilApi.class)
 */
public interface PerfilApi {

    @GET("api/v1/usuarios/me")
    Call<PerfilModels.MiPerfil> obtenerMiPerfil();

    @PUT("api/v1/usuarios/me")
    Call<PerfilModels.ActualizarPerfilResponse> actualizarMiPerfil(@Body PerfilModels.ActualizarPerfilRequest request);

    @Multipart
    @POST("api/v1/usuarios/me/foto")
    Call<PerfilModels.MiPerfil> actualizarFoto(@Part MultipartBody.Part foto);

    @GET("api/v1/usuarios/{id}/perfil-publico")
    Call<PerfilModels.PerfilPublico> obtenerPerfilPublico(@Path("id") long usuarioId);
}
