package com.example.ronda.oferta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.ronda.ApiClient;
import com.example.ronda.ConnectivityObserver;
import com.example.ronda.Formato;
import com.example.ronda.PuntoEntregaActivity;
import com.example.ronda.util.ApiCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * Todas las acciones sobre ofertas en un solo lugar: ofertar, aceptar, rechazar,
 * contraofertar y ver el punto de entrega. Las usan el detalle de la publicación
 * y "Mis ofertas", así cada regla (confirmaciones, conexión, mensajes) se escribe una sola vez.
 */
public final class OfertaAcciones {

    /** Se ejecuta cuando el backend devuelve la oferta actualizada. */
    public interface AlTerminar {
        void onOfertaActualizada(OfertaModels.Oferta oferta);
    }

    private OfertaAcciones() { }

    /** Oferta inicial desde el detalle de una publicación. */
    public static void ofertar(Activity activity, long publicacionId, double precioPublicado) {
        if (!hayConexion(activity)) {
            return;
        }
        PropuestaPrecioDialog.mostrar(activity, "Hacer una oferta", "Enviar oferta",
                "Precio publicado", precioPublicado, (precio, mensaje) ->
                        api().ofertar(publicacionId, new OfertaModels.PropuestaRequest(precio, mensaje))
                                .enqueue(new ApiCallback<OfertaModels.Oferta>(activity, "No se pudo enviar la oferta") {
                                    @Override
                                    protected void onExito(OfertaModels.Oferta oferta) {
                                        Snackbar.make(activity.findViewById(android.R.id.content),
                                                        "Oferta enviada", Snackbar.LENGTH_LONG)
                                                .setAction("Ver mis ofertas", v ->
                                                        activity.startActivity(MisOfertasActivity.crearIntent(activity)))
                                                .show();
                                    }
                                }));
    }

    public static void aceptar(Activity activity, OfertaModels.Oferta oferta, AlTerminar alTerminar) {
        confirmar(activity, "¿Aceptar la oferta?",
                "Cerrás el trato por " + Formato.precio(oferta.getPrecio())
                        + ". Si hay otras ofertas pendientes por esta publicación, se rechazan automáticamente.",
                "Aceptar",
                () -> api().aceptar(oferta.getId())
                        .enqueue(callback(activity, "No se pudo aceptar la oferta", "Oferta aceptada", alTerminar)));
    }

    public static void rechazar(Activity activity, OfertaModels.Oferta oferta, AlTerminar alTerminar) {
        confirmar(activity, "¿Rechazar la oferta?", "La negociación termina sin acuerdo.", "Rechazar",
                () -> api().rechazar(oferta.getId())
                        .enqueue(callback(activity, "No se pudo rechazar la oferta", "Oferta rechazada", alTerminar)));
    }

    public static void contraofertar(Activity activity, OfertaModels.Oferta oferta, AlTerminar alTerminar) {
        if (!hayConexion(activity)) {
            return;
        }
        PropuestaPrecioDialog.mostrar(activity, "Contraofertar", "Enviar contraoferta",
                "Propuesta actual", oferta.getPrecio(), (precio, mensaje) ->
                        api().contraofertar(oferta.getId(), new OfertaModels.PropuestaRequest(precio, mensaje))
                                .enqueue(callback(activity, "No se pudo enviar la contraoferta",
                                        "Contraoferta enviada", alTerminar)));
    }

    /** Abre la pantalla de punto de entrega (punto 8) con la dirección de la oferta aceptada. */
    public static void verPuntoEntrega(Context context, OfertaModels.Oferta oferta) {
        OfertaModels.Entrega entrega = oferta.getEntrega();
        if (entrega == null) {
            Toast.makeText(context, "El punto de entrega se ve cuando la oferta está aceptada",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(context, PuntoEntregaActivity.class)
                .putExtra(PuntoEntregaActivity.EXTRA_TITULO, oferta.getPublicacion().getTitulo())
                .putExtra(PuntoEntregaActivity.EXTRA_DIRECCION, entrega.getDireccion())
                .putExtra(PuntoEntregaActivity.EXTRA_LATITUD, entrega.getLatitud())
                .putExtra(PuntoEntregaActivity.EXTRA_LONGITUD, entrega.getLongitud());
        context.startActivity(intent);
    }

    // ---------- helpers ----------

    private static OfertaApi api() {
        return ApiClient.crearServicio(OfertaApi.class);
    }

    private static ApiCallback<OfertaModels.Oferta> callback(Activity activity, String mensajeError,
                                                             String mensajeExito, AlTerminar alTerminar) {
        return new ApiCallback<OfertaModels.Oferta>(activity, mensajeError) {
            @Override
            protected void onExito(OfertaModels.Oferta oferta) {
                Toast.makeText(activity, mensajeExito, Toast.LENGTH_SHORT).show();
                alTerminar.onOfertaActualizada(oferta);
            }
        };
    }

    /** Pide confirmación y, si hay conexión, ejecuta la acción. */
    private static void confirmar(Activity activity, String titulo, String mensaje, String textoConfirmar,
                                  Runnable accion) {
        if (!hayConexion(activity)) {
            return;
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(textoConfirmar, (dialog, which) -> accion.run())
                .show();
    }

    /** Las acciones de ofertas requieren conexión (punto 6 del TPO). */
    private static boolean hayConexion(Context context) {
        if (ConnectivityObserver.isOnline(context)) {
            return true;
        }
        Toast.makeText(context, "Necesitás conexión a internet para continuar", Toast.LENGTH_LONG).show();
        return false;
    }
}
