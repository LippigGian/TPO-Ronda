package com.example.ronda.favorito;

import android.app.Activity;
import android.widget.Toast;

import com.example.ronda.ApiClient;
import com.example.ronda.util.ApiCallback;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Todas las acciones sobre favoritos y búsquedas guardadas en un solo lugar (punto 10),
 * usadas desde el Home, el Detalle y "Mis favoritos".
 */
public final class FavoritoAcciones {

    public interface AlTerminar {
        void onListo();
    }

    public interface AlGuardarBusqueda {
        void onGuardada(FavoritoModels.BusquedaGuardadaItem item);
    }

    private FavoritoAcciones() { }

    public static void marcar(Activity activity, long publicacionId, AlTerminar alTerminar) {
        api().marcar(publicacionId).enqueue(callback(activity, "No se pudo agregar a favoritos", alTerminar));
    }

    public static void desmarcar(Activity activity, long publicacionId, AlTerminar alTerminar) {
        api().desmarcar(publicacionId).enqueue(callback(activity, "No se pudo quitar de favoritos", alTerminar));
    }

    public static void guardarBusqueda(Activity activity, FavoritoModels.GuardarBusquedaRequest request,
                                       AlGuardarBusqueda alTerminar) {
        api().guardarBusqueda(request).enqueue(
                new ApiCallback<FavoritoModels.BusquedaGuardadaItem>(activity, "No se pudo guardar la búsqueda") {
                    @Override
                    protected void onExito(FavoritoModels.BusquedaGuardadaItem respuesta) {
                        Toast.makeText(activity, "Búsqueda guardada", Toast.LENGTH_SHORT).show();
                        alTerminar.onGuardada(respuesta);
                    }
                });
    }

    public static void eliminarBusqueda(Activity activity, long id, AlTerminar alTerminar) {
        api().eliminarBusqueda(id)
                .enqueue(callback(activity, "No se pudo eliminar la búsqueda guardada", alTerminar));
    }

    /** Best-effort: limpia el indicador de novedad sin feedback visual ni reintentos. */
    public static void marcarVisto(long publicacionId) {
        api().marcarVisto(publicacionId).enqueue(sinRespuesta());
    }

    /** Best-effort: idem, para cuando el usuario abre los resultados de una búsqueda guardada. */
    public static void marcarBusquedaVista(long id) {
        api().marcarBusquedaVista(id).enqueue(sinRespuesta());
    }

    // ---------- helpers ----------

    private static FavoritoApi api() {
        return ApiClient.crearServicio(FavoritoApi.class);
    }

    private static ApiCallback<Void> callback(Activity activity, String mensajeError, AlTerminar alTerminar) {
        return new ApiCallback<Void>(activity, mensajeError) {
            @Override
            protected void onExito(Void respuesta) {
                alTerminar.onListo();
            }
        };
    }

    private static Callback<Void> sinRespuesta() {
        return new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) { }
            @Override public void onFailure(Call<Void> call, Throwable error) { }
        };
    }
}
