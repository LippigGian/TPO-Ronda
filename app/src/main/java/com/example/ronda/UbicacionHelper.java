package com.example.ronda;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

/** Ubicación aproximada del dispositivo para el filtro de cercanía. */
public final class UbicacionHelper {

    private UbicacionHelper() { }

    public static boolean tienePermiso(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Última ubicación conocida, o null si no hay permiso o el dispositivo todavía no obtuvo ninguna. */
    @SuppressWarnings("MissingPermission")
    public static Location ultimaUbicacion(Context context) {
        if (!tienePermiso(context)) {
            return null;
        }
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location mejor = null;
        for (String proveedor : manager.getProviders(true)) {
            Location candidata;
            try {
                candidata = manager.getLastKnownLocation(proveedor);
            } catch (SecurityException | IllegalArgumentException error) {
                continue;
            }
            if (candidata != null && (mejor == null || candidata.getTime() > mejor.getTime())) {
                mejor = candidata;
            }
        }
        return mejor;
    }
}
