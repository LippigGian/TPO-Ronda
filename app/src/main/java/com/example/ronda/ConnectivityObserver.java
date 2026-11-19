package com.example.ronda;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

/**
 * Punto 6: estado de la conexión a internet.
 *
 * Escucha la red por defecto del dispositivo y avisa solo cuando el estado cambia de verdad
 * (con internet validado o sin internet), siempre en el hilo principal. Así una pantalla puede
 * deshabilitar sus acciones al perder la conexión y refrescar sus datos al recuperarla.
 */
public final class ConnectivityObserver {

    public interface Listener {
        void onStatusChanged(boolean online);
    }

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());
    private ConnectivityManager.NetworkCallback callback;
    private boolean ultimoEstado;

    public ConnectivityObserver(Context context) {
        this.context = context.getApplicationContext();
        connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /** Chequeo puntual, sincrónico: ¿hay internet ahora mismo? **/
    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /** Empieza a escuchar cambios de conectividad (llamar en onStart). **/
    public void start(Listener listener) {
        stop();
        ultimoEstado = isOnline(context);
        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                revisar(listener);
            }

            // La red puede aparecer antes de tener internet validado: el cambio real llega acá.
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                revisar(listener);
            }

            @Override
            public void onLost(Network network) {
                revisar(listener);
            }
        };
        connectivityManager.registerDefaultNetworkCallback(callback);
    }

    /** Dejar de escuchar (llamar en onStop para no leakear). **/
    public void stop() {
        if (callback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(callback);
            } catch (IllegalArgumentException yaDesregistrado) {
                // Nada que hacer.
            }
            callback = null;
        }
    }

    /** Recalcula el estado en el hilo principal y avisa solo si cambió. */
    private void revisar(Listener listener) {
        hiloPrincipal.post(() -> {
            if (callback == null) {
                return; // la pantalla ya dejó de escuchar
            }
            boolean ahora = isOnline(context);
            if (ahora != ultimoEstado) {
                ultimoEstado = ahora;
                listener.onStatusChanged(ahora);
            }
        });
    }
}
