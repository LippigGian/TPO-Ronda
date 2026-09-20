package com.example.ronda.oferta;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.ronda.ApiClient;
import com.example.ronda.R;

/** Completa una fila de item_oferta.xml con los datos de una oferta. */
final class OfertaItemBinder {

    /** Lo que puede hacer el usuario desde una fila. Lo implementa la pantalla que muestra la lista. */
    interface Acciones {
        void aceptar(OfertaModels.Oferta oferta);
        void rechazar(OfertaModels.Oferta oferta);
        void contraofertar(OfertaModels.Oferta oferta);
        void verPuntoEntrega(OfertaModels.Oferta oferta);
        void abrirPublicacion(OfertaModels.Oferta oferta);
    }

    private OfertaItemBinder() { }

    /** @param online sin conexión los botones quedan deshabilitados (punto 6). */
    static void completar(Context context, View fila, OfertaModels.Oferta oferta, boolean online,
                          Acciones acciones) {
        ((TextView) fila.findViewById(R.id.tvTituloOferta)).setText(oferta.getPublicacion().getTitulo());
        ((TextView) fila.findViewById(R.id.tvContraparteOferta)).setText(OfertaFormatter.contraparte(oferta));
        ((TextView) fila.findViewById(R.id.tvPreciosOferta)).setText(OfertaFormatter.precios(oferta));
        ((TextView) fila.findViewById(R.id.tvSituacionOferta)).setText(OfertaFormatter.situacion(oferta));

        mostrarEstado(context, fila.findViewById(R.id.tvEstadoOferta), oferta.getEstado());
        mostrarMensaje(fila.findViewById(R.id.tvMensajeOferta), oferta.getMensaje());
        mostrarFoto(context, fila.findViewById(R.id.ivFotoOferta), oferta.getPublicacion().getFoto());

        // Responder: solo si la oferta está pendiente y me toca a mí.
        View grupoRespuesta = fila.findViewById(R.id.grupoRespuestaOferta);
        grupoRespuesta.setVisibility(oferta.isPuedoResponder() ? View.VISIBLE : View.GONE);
        configurarBoton(fila.findViewById(R.id.btnAceptarOferta), online, v -> acciones.aceptar(oferta));
        configurarBoton(fila.findViewById(R.id.btnRechazarOferta), online, v -> acciones.rechazar(oferta));
        configurarBoton(fila.findViewById(R.id.btnContraofertar), online, v -> acciones.contraofertar(oferta));

        // Punto de entrega: solo el comprador y solo con la oferta aceptada.
        View btnEntrega = fila.findViewById(R.id.btnVerPuntoEntrega);
        btnEntrega.setVisibility(oferta.estaAceptada() && oferta.soyComprador() ? View.VISIBLE : View.GONE);
        btnEntrega.setOnClickListener(v -> acciones.verPuntoEntrega(oferta));

        fila.setOnClickListener(v -> acciones.abrirPublicacion(oferta));
    }

    private static void mostrarEstado(Context context, TextView chip, String estado) {
        chip.setText(OfertaFormatter.estado(estado));
        chip.setTextColor(ContextCompat.getColor(context, OfertaFormatter.colorEstado(estado)));
        chip.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, OfertaFormatter.fondoEstado(estado))));
    }

    private static void mostrarMensaje(TextView tvMensaje, String mensaje) {
        boolean hayMensaje = !TextUtils.isEmpty(mensaje);
        tvMensaje.setVisibility(hayMensaje ? View.VISIBLE : View.GONE);
        if (hayMensaje) {
            tvMensaje.setText("“" + mensaje + "”");
        }
    }

    private static void mostrarFoto(Context context, ImageView destino, String rutaRelativa) {
        Glide.with(context)
                .load(rutaRelativa == null ? null : ApiClient.imageUrl(rutaRelativa))
                .centerCrop()
                .into(destino);
    }

    private static void configurarBoton(View boton, boolean online, View.OnClickListener alTocar) {
        boton.setEnabled(online);
        boton.setOnClickListener(alTocar);
    }
}
