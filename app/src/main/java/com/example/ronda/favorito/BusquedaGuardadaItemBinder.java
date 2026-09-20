package com.example.ronda.favorito;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.example.ronda.Formato;
import com.example.ronda.R;

import java.util.ArrayList;
import java.util.List;

/** Completa una fila de item_busqueda_guardada.xml con los datos de una búsqueda guardada. */
final class BusquedaGuardadaItemBinder {

    interface Acciones {
        void eliminar(FavoritoModels.BusquedaGuardadaItem busqueda);
        void verResultados(FavoritoModels.BusquedaGuardadaItem busqueda);
    }

    private BusquedaGuardadaItemBinder() { }

    static void completar(Context context, View fila, FavoritoModels.BusquedaGuardadaItem busqueda,
                          Acciones acciones) {
        ((TextView) fila.findViewById(R.id.tvNombreBusqueda)).setText(busqueda.getNombre());
        ((TextView) fila.findViewById(R.id.tvFiltrosBusqueda)).setText(resumenFiltros(busqueda));

        TextView tvNovedad = fila.findViewById(R.id.tvNovedadBusqueda);
        long nuevas = busqueda.getCantidadNuevas();
        if (nuevas > 0) {
            tvNovedad.setVisibility(View.VISIBLE);
            tvNovedad.setText(nuevas == 1 ? "1 publicación nueva" : nuevas + " publicaciones nuevas");
        } else {
            tvNovedad.setVisibility(View.GONE);
        }

        fila.findViewById(R.id.btnEliminarBusqueda).setOnClickListener(v -> acciones.eliminar(busqueda));
        fila.setOnClickListener(v -> acciones.verResultados(busqueda));
    }

    /** Arma un resumen legible de los filtros guardados, ej: "Electrodomésticos · hasta $ 100.000". */
    private static String resumenFiltros(FavoritoModels.BusquedaGuardadaItem busqueda) {
        List<String> partes = new ArrayList<>();
        if (busqueda.getQ() != null && !busqueda.getQ().isEmpty()) {
            partes.add("\"" + busqueda.getQ() + "\"");
        }
        if (busqueda.getCategoria() != null) {
            partes.add(busqueda.getCategoria());
        }
        if (busqueda.getPrecioMin() != null && busqueda.getPrecioMax() != null) {
            partes.add(Formato.precio(busqueda.getPrecioMin()) + " - " + Formato.precio(busqueda.getPrecioMax()));
        } else if (busqueda.getPrecioMax() != null) {
            partes.add("hasta " + Formato.precio(busqueda.getPrecioMax()));
        } else if (busqueda.getPrecioMin() != null) {
            partes.add("desde " + Formato.precio(busqueda.getPrecioMin()));
        }
        if (busqueda.getEstadoArticulo() != null) {
            partes.add(Formato.estadoArticulo(busqueda.getEstadoArticulo()));
        }
        if (busqueda.getRadioKm() != null) {
            partes.add("a " + Math.round(busqueda.getRadioKm()) + " km");
        }
        return partes.isEmpty() ? "Sin filtros adicionales" : String.join(" · ", partes);
    }
}
