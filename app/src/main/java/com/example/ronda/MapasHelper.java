package com.example.ronda;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.util.Locale;

public final class MapasHelper {

    private MapasHelper() {}

    /** Atajo para usar directo con la publicación que devuelve la API. */
    public static void abrirComoLlegar(Context context, ApiClient.PublicacionResponse publicacion) {
        if (publicacion == null) {
            Toast.makeText(context, "No hay punto de entrega definido", Toast.LENGTH_SHORT).show();
            return;
        }
        abrirComoLlegar(context, publicacion.getDireccion(),
                publicacion.getLatitud(), publicacion.getLongitud());
    }

    /**
     * Abre la navegación hacia el punto de entrega.
     * Usa coordenadas si las hay (más preciso) y si no, la dirección en texto.
     * Orden de intentos: Google Maps en modo navegación -> app de mapas por defecto -> navegador.
     */
    public static void abrirComoLlegar(Context context, String direccion, double latitud, double longitud) {
        boolean tieneCoordenadas = latitud != 0.0 || longitud != 0.0;
        boolean tieneDireccion = direccion != null && !direccion.trim().isEmpty();

        if (!tieneCoordenadas && !tieneDireccion) {
            Toast.makeText(context, "No hay punto de entrega definido", Toast.LENGTH_SHORT).show();
            return;
        }

        String destino = tieneCoordenadas
                ? String.format(Locale.US, "%.6f,%.6f", latitud, longitud)
                : Uri.encode(direccion.trim());

        String geoUri = "geo:0,0?q=" + destino;
        if (tieneCoordenadas && tieneDireccion) {
            geoUri += "(" + Uri.encode(direccion.trim()) + ")";
        }

        Intent[] intentos = new Intent[] {
                // 1) Google Maps directo en modo navegación
                new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + destino + "&mode=d"))
                        .setPackage("com.google.android.apps.maps"),
                // 2) Cualquier app de mapas del dispositivo
                new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri)),
                // 3) Último recurso: Google Maps en el navegador
                new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "https://www.google.com/maps/dir/?api=1&destination=" + destino + "&travelmode=driving"))
        };

        for (Intent intent : intentos) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            } catch (ActivityNotFoundException ignored) {
                // probamos el siguiente
            }
        }

        Toast.makeText(context, "No se encontró ninguna app de mapas", Toast.LENGTH_SHORT).show();
    }
}
