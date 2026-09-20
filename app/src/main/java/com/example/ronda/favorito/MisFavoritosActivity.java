package com.example.ronda.favorito;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ronda.ApiClient;
import com.example.ronda.ConnectivityObserver;
import com.example.ronda.DetallePublicacionActivity;
import com.example.ronda.HomeActivity;
import com.example.ronda.R;
import com.example.ronda.util.ApiCallback;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Punto 10: "Mis favoritos". Favoritos y búsquedas guardadas en un mismo lugar, con
 * el indicador de novedad (cambio de precio / publicaciones nuevas) de cada una.
 */
public class MisFavoritosActivity extends AppCompatActivity
        implements FavoritoItemBinder.Acciones, BusquedaGuardadaItemBinder.Acciones {

    private static final int TAB_FAVORITOS = 0;
    private static final int TAB_BUSQUEDAS = 1;

    private FavoritoApi api;
    private TabLayout tabs;
    private LinearLayout contenedor;
    private TextView tvVacio;
    private TextView tvAvisoOffline;
    private ProgressBar progreso;

    private List<FavoritoModels.FavoritoItem> favoritos;
    private List<FavoritoModels.BusquedaGuardadaItem> busquedas;
    private boolean online = true;

    public static Intent crearIntent(Context context) {
        return new Intent(context, MisFavoritosActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mis_favoritos);
        aplicarInsets();

        api = ApiClient.crearServicio(FavoritoApi.class);
        tabs = findViewById(R.id.tabsFavoritos);
        contenedor = findViewById(R.id.contenedorFavoritos);
        tvVacio = findViewById(R.id.tvSinFavoritos);
        tvAvisoOffline = findViewById(R.id.tvAvisoOfflineFavoritos);
        progreso = findViewById(R.id.progresoFavoritos);

        tabs.addTab(tabs.newTab().setText("Favoritos"));
        tabs.addTab(tabs.newTab().setText("Búsquedas guardadas"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { mostrarListaActual(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargar();
    }

    // ---------- Carga de datos ----------

    private void cargar() {
        online = ConnectivityObserver.isOnline(this);
        tvAvisoOffline.setVisibility(online ? View.GONE : View.VISIBLE);
        if (!online) {
            mostrarListaActual(); // sin conexión se ve lo último cargado
            return;
        }

        if (favoritos == null && busquedas == null) {
            progreso.setVisibility(View.VISIBLE);
        }
        cargarFavoritos();
        cargarBusquedas();
    }

    private void cargarFavoritos() {
        api.listarFavoritos().enqueue(new ApiCallback<List<FavoritoModels.FavoritoItem>>(this,
                "No se pudieron cargar tus favoritos") {
            @Override
            protected void onExito(List<FavoritoModels.FavoritoItem> respuesta) {
                favoritos = respuesta;
                actualizarPestania(TAB_FAVORITOS, "Favoritos", favoritos.size(), 0);
                if (tabs.getSelectedTabPosition() == TAB_FAVORITOS) {
                    mostrarListaActual();
                }
            }

            @Override
            protected void onFinalizar() {
                progreso.setVisibility(View.GONE);
            }
        });
    }

    private void cargarBusquedas() {
        api.listarBusquedas().enqueue(new ApiCallback<List<FavoritoModels.BusquedaGuardadaItem>>(this,
                "No se pudieron cargar tus búsquedas guardadas") {
            @Override
            protected void onExito(List<FavoritoModels.BusquedaGuardadaItem> respuesta) {
                busquedas = respuesta;
                long conNovedades = respuesta.stream().filter(b -> b.getCantidadNuevas() > 0).count();
                actualizarPestania(TAB_BUSQUEDAS, "Búsquedas guardadas", busquedas.size(), conNovedades);
                if (tabs.getSelectedTabPosition() == TAB_BUSQUEDAS) {
                    mostrarListaActual();
                }
            }

            @Override
            protected void onFinalizar() {
                progreso.setVisibility(View.GONE);
            }
        });
    }

    // ---------- Pantalla ----------

    private void actualizarPestania(int posicion, String titulo, int cantidad, long conNovedades) {
        TabLayout.Tab tab = tabs.getTabAt(posicion);
        if (tab == null) {
            return;
        }
        tab.setText(titulo + " (" + cantidad + ")");
        if (conNovedades > 0) {
            tab.getOrCreateBadge().setNumber((int) conNovedades);
        } else {
            tab.removeBadge();
        }
    }

    private void mostrarListaActual() {
        contenedor.removeAllViews();
        boolean pestaniaFavoritos = tabs.getSelectedTabPosition() != TAB_BUSQUEDAS;
        LayoutInflater inflater = LayoutInflater.from(this);

        if (pestaniaFavoritos) {
            List<FavoritoModels.FavoritoItem> items = favoritos != null ? favoritos : new ArrayList<>();
            tvVacio.setVisibility(favoritos != null && items.isEmpty() ? View.VISIBLE : View.GONE);
            tvVacio.setText("Todavía no marcaste publicaciones como favoritas.");
            for (FavoritoModels.FavoritoItem favorito : items) {
                View fila = inflater.inflate(R.layout.item_favorito, contenedor, false);
                FavoritoItemBinder.completar(this, fila, favorito, this);
                contenedor.addView(fila);
            }
        } else {
            List<FavoritoModels.BusquedaGuardadaItem> items = busquedas != null ? busquedas : new ArrayList<>();
            tvVacio.setVisibility(busquedas != null && items.isEmpty() ? View.VISIBLE : View.GONE);
            tvVacio.setText("Todavía no guardaste ninguna búsqueda. Podés hacerlo desde los filtros del Home.");
            for (FavoritoModels.BusquedaGuardadaItem busqueda : items) {
                View fila = inflater.inflate(R.layout.item_busqueda_guardada, contenedor, false);
                BusquedaGuardadaItemBinder.completar(this, fila, busqueda, this);
                contenedor.addView(fila);
            }
        }
    }

    // ---------- Acciones de favoritos (FavoritoItemBinder.Acciones) ----------

    @Override
    public void quitar(FavoritoModels.FavoritoItem favorito) {
        FavoritoAcciones.desmarcar(this, favorito.getPublicacionId(), () -> {
            favoritos.remove(favorito);
            actualizarPestania(TAB_FAVORITOS, "Favoritos", favoritos.size(), 0);
            mostrarListaActual();
        });
    }

    @Override
    public void abrirPublicacion(FavoritoModels.FavoritoItem favorito) {
        startActivity(DetallePublicacionActivity.crearIntent(this, favorito.getPublicacionId()));
    }

    // ---------- Acciones de búsquedas guardadas (BusquedaGuardadaItemBinder.Acciones) ----------

    @Override
    public void eliminar(FavoritoModels.BusquedaGuardadaItem busqueda) {
        FavoritoAcciones.eliminarBusqueda(this, busqueda.getId(), () -> {
            busquedas.remove(busqueda);
            long conNovedades = busquedas.stream().filter(b -> b.getCantidadNuevas() > 0).count();
            actualizarPestania(TAB_BUSQUEDAS, "Búsquedas guardadas", busquedas.size(), conNovedades);
            mostrarListaActual();
        });
    }

    @Override
    public void verResultados(FavoritoModels.BusquedaGuardadaItem busqueda) {
        FavoritoAcciones.marcarBusquedaVista(busqueda.getId());
        startActivity(HomeActivity.crearIntentConFiltros(this, busqueda.getQ(), busqueda.getCategoria(),
                busqueda.getPrecioMin(), busqueda.getPrecioMax(), busqueda.getEstadoArticulo()));
    }

    private void aplicarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
