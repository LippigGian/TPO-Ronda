package com.example.ronda;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

public final class ConnectivityObserver {

    public interface Listener {
        void onStatusChanged(boolean online);
    }

    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback callback;

    public ConnectivityObserver(Context context) {
        connectivityManager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
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

    /** Empieza a escuchar cambios de conectividad (llamar en onStart/onResume). **/
    public void start(Listener listener) {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                listener.onStatusChanged(true);
            }

            @Override
            public void onLost(Network network) {
                listener.onStatusChanged(false);
            }
        };

        connectivityManager.registerNetworkCallback(request, callback);
    }

    /** Dejar de escuchar (llamar en onStop/onPause para no leakear). **/
    public void stop() {
        if (callback != null) {
            connectivityManager.unregisterNetworkCallback(callback);
            callback = null;
        }
    }
}
