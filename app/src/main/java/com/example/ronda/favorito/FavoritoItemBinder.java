package com.example.ronda.favorito;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.ronda.ApiClient;
import com.example.ronda.Formato;
import com.example.ronda.R;

/** Completa una fila de item_favorito.xml con los datos de un favorito. */
final class FavoritoItemBinder {

    interface Acciones {
        void quitar(FavoritoModels.FavoritoItem favorito);
        void abrirPublicacion(FavoritoModels.FavoritoItem favorito);
    }

    private FavoritoItemBinder() { }

    static void completar(Context context, View fila, FavoritoModels.FavoritoItem favorito, Acciones acciones) {
        ((TextView) fila.findViewById(R.id.tvTituloFavorito)).setText(favorito.getTitulo());
        ((TextView) fila.findViewById(R.id.tvPrecioFavorito)).setText(Formato.precio(favorito.getPrecioActual()));

        TextView tvAnterior = fila.findViewById(R.id.tvPrecioAnteriorFavorito);
        TextView tvNovedad = fila.findViewById(R.id.tvNovedadFavorito);
        if (favorito.isCambioPrecio()) {
            tvAnterior.setVisibility(View.VISIBLE);
            tvAnterior.setText("Antes " + Formato.precio(favorito.getPrecioGuardado()));
            tvNovedad.setVisibility(View.VISIBLE);
            tvNovedad.setText(favorito.getPrecioActual() < favorito.getPrecioGuardado()
                    ? "Bajó de precio" : "Subió de precio");
        } else {
            tvAnterior.setVisibility(View.GONE);
            tvNovedad.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(favorito.getFoto() == null ? null : ApiClient.imageUrl(favorito.getFoto()))
                .centerCrop()
                .into((ImageView) fila.findViewById(R.id.ivFotoFavorito));

        fila.findViewById(R.id.btnQuitarFavorito).setOnClickListener(v -> acciones.quitar(favorito));
        fila.setOnClickListener(v -> acciones.abrirPublicacion(favorito));
    }
}
