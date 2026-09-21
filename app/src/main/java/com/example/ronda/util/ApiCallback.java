package com.example.ronda.util;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.example.ronda.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Callback de Retrofit reutilizable: centraliza el manejo de errores HTTP y de red
 * para que cada pantalla solo escriba que hacer cuando la respuesta es exitosa.
 *
 * Uso:
 *   api.algo().enqueue(new ApiCallback<Tipo>(this, "Mensaje si falla") {
 *       protected void onExito(Tipo respuesta) { ... }
 *   });
 */
public abstract class ApiCallback<T> implements Callback<T> {
    private final Context context;
    private final String mensajeError;

    protected ApiCallback(Context context, String mensajeError) {
        this.context = context;
        this.mensajeError = mensajeError;
    }

    /** Se ejecuta cuando el servidor respondio 2xx. */
    protected abstract void onExito(T respuesta);

    /** Se ejecuta ante error HTTP o de red. Por defecto muestra un Toast; se puede sobrescribir. */
    protected void onError(String mensaje) {
        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show();
    }

    /** Se ejecuta siempre al final (exito o error). Util para ocultar loaders o rehabilitar botones. */
    protected void onFinalizar() { }

    @Override
    public final void onResponse(Call<T> call, Response<T> response) {
        if (pantallaCerrada()) {
            return;
        }
        if (response.isSuccessful()) {
            onExito(response.body());
        } else {
            onError(ApiClient.errorMessage(response, mensajeError));
        }
        onFinalizar();
    }

    @Override
    public final void onFailure(Call<T> call, Throwable error) {
        if (pantallaCerrada()) {
            return;
        }
        onError("No se pudo conectar con el servidor");
        onFinalizar();
    }

    /** Evita tocar vistas de una pantalla que el usuario ya cerro mientras esperaba la respuesta. */
    private boolean pantallaCerrada() {
        return context instanceof Activity && ((Activity) context).isFinishing();
    }
}
