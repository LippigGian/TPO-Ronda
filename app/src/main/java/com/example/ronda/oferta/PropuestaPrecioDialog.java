package com.example.ronda.oferta;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.ronda.Formato;
import com.example.ronda.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Diálogo reutilizable para proponer un precio con mensaje opcional.
 * Se usa para la oferta inicial (desde el detalle) y para las contraofertas (desde Mis ofertas).
 */
public final class PropuestaPrecioDialog {

    public interface AlConfirmar {
        void onPropuesta(double precio, String mensaje);
    }

    private static final double TOLERANCIA_CENTAVOS = 0.005;

    private PropuestaPrecioDialog() { }

    /**
     * @param etiquetaReferencia texto del precio de referencia, ej: "Precio publicado"
     * @param precioReferencia   la propuesta tiene que ser distinta a este precio
     */
    public static void mostrar(Context context, String titulo, String textoConfirmar,
                               String etiquetaReferencia, double precioReferencia, AlConfirmar alConfirmar) {
        View vista = LayoutInflater.from(context).inflate(R.layout.dialog_propuesta_precio, null);
        TextView tvReferencia = vista.findViewById(R.id.tvPrecioReferencia);
        TextInputLayout tilPrecio = vista.findViewById(R.id.tilPrecioPropuesta);
        TextInputEditText etPrecio = vista.findViewById(R.id.etPrecioPropuesta);
        TextInputEditText etMensaje = vista.findViewById(R.id.etMensajePropuesta);

        tvReferencia.setText(etiquetaReferencia + ": " + Formato.precio(precioReferencia));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(titulo)
                .setView(vista)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(textoConfirmar, null) // el listener se asigna abajo para poder validar
                .create();

        // Se reemplaza el click del botón para que el diálogo NO se cierre si hay errores.
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Double precio = leerPrecio(etPrecio);
            if (precio == null || precio <= 0) {
                tilPrecio.setError("Ingresá un precio mayor a 0");
                return;
            }
            if (Math.abs(precio - precioReferencia) < TOLERANCIA_CENTAVOS) {
                tilPrecio.setError("Proponé un precio distinto al actual");
                return;
            }
            String mensaje = texto(etMensaje);
            alConfirmar.onPropuesta(precio, TextUtils.isEmpty(mensaje) ? null : mensaje);
            dialog.dismiss();
        }));

        dialog.show();
    }

    /** Acepta "80000", "80000.50" o "80000,50". Devuelve null si no es un número. */
    private static Double leerPrecio(TextInputEditText campo) {
        String valor = texto(campo).replace(",", ".");
        if (valor.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
