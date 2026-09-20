package com.example.ronda.favorito;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ronda.ApiClient;
import com.example.ronda.ConnectivityObserver;
import com.example.ronda.DetallePublicacionActivity;
import com.example.ronda.FiltrosDialogHelper;
import com.example.ronda.FiltrosHome;
import com.example.ronda.HomeActivity;
import com.example.ronda.R;
import com.example.ronda.UbicacionHelper;
import com.example.ronda.util.ApiCallback;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private final List<String> categorias = new ArrayList<>();
    private boolean online = true;

    private ActivityResultLauncher<String> pedirPermisoUbicacion;
    /** Se retoma cuando el usuario responde al permiso de ubicación pedido al editar una búsqueda. */
    private Runnable pendienteTrasUbicacion;

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

        pedirPermisoUbicacion = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), concedido -> {
                    Runnable pendiente = pendienteTrasUbicacion;
                    pendienteTrasUbicacion = null;
                    if (!concedido) {
                        Toast.makeText(this, "Sin permiso de ubicación no pudimos aplicar la cercanía",
                                Toast.LENGTH_LONG).show();
                    }
                    if (pendiente != null) {
                        pendiente.run();
                    }
                });

        cargarCategorias();
    }

    /** Best-effort: sin categorías el editor de filtros queda solo con "Todas". */
    private void cargarCategorias() {
        ApiClient.api().getCategorias().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categorias.clear();
                    categorias.addAll(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable error) { }
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
    public void editar(FavoritoModels.BusquedaGuardadaItem busqueda) {
        mostrarDialogoEditar(busqueda);
    }

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

    /**
     * Punto 10: editar una búsqueda guardada reutiliza dialog_filtros.xml (mismo layout que Home),
     * pero oculta la sección de "guardar como nueva" y mantiene el nombre existente.
     */
    private void mostrarDialogoEditar(FavoritoModels.BusquedaGuardadaItem busqueda) {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_filtros, null);
        AutoCompleteTextView actvCategoria = vista.findViewById(R.id.actvCategoria);
        TextInputEditText etPrecioMin = vista.findViewById(R.id.etPrecioMin);
        TextInputEditText etPrecioMax = vista.findViewById(R.id.etPrecioMax);
        ChipGroup cgEstado = vista.findViewById(R.id.cgEstado);
        MaterialSwitch swCercania = vista.findViewById(R.id.swCercania);
        ChipGroup cgRadio = vista.findViewById(R.id.cgRadio);
        vista.findViewById(R.id.cbGuardarBusqueda).setVisibility(View.GONE);
        vista.findViewById(R.id.tilNombreBusqueda).setVisibility(View.GONE);

        List<String> opciones = new ArrayList<>();
        opciones.add(getString(R.string.filtro_todas));
        opciones.addAll(categorias);
        actvCategoria.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, opciones));
        actvCategoria.setText(busqueda.getCategoria() != null ? busqueda.getCategoria() : opciones.get(0), false);

        if (busqueda.getPrecioMin() != null) {
            etPrecioMin.setText(FiltrosDialogHelper.numeroSinDecimales(busqueda.getPrecioMin()));
        }
        if (busqueda.getPrecioMax() != null) {
            etPrecioMax.setText(FiltrosDialogHelper.numeroSinDecimales(busqueda.getPrecioMax()));
        }
        cgEstado.check(FiltrosDialogHelper.chipDeEstado(busqueda.getEstadoArticulo()));

        double radioActual = busqueda.getRadioKm() != null ? busqueda.getRadioKm() : FiltrosHome.RADIO_KM_POR_DEFECTO;
        swCercania.setChecked(busqueda.isCercania());
        cgRadio.setVisibility(busqueda.isCercania() ? View.VISIBLE : View.GONE);
        cgRadio.check(FiltrosDialogHelper.chipDeRadio(radioActual));
        swCercania.setOnCheckedChangeListener((boton, marcado) ->
                cgRadio.setVisibility(marcado ? View.VISIBLE : View.GONE));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.filtros_titulo)
                .setView(vista)
                .setPositiveButton(R.string.filtro_aplicar, (dialogo, boton) -> {
                    String elegida = actvCategoria.getText().toString();
                    String categoria = categorias.contains(elegida) ? elegida : null;
                    Double precioMin = FiltrosDialogHelper.leerNumero(etPrecioMin);
                    Double precioMax = FiltrosDialogHelper.leerNumero(etPrecioMax);
                    if (precioMin != null && precioMax != null && precioMin > precioMax) {
                        Double intercambio = precioMin;
                        precioMin = precioMax;
                        precioMax = intercambio;
                    }
                    String estadoArticulo = FiltrosDialogHelper.estadoDeChip(cgEstado.getCheckedChipId());
                    boolean cercania = swCercania.isChecked();
                    double radioKm = FiltrosDialogHelper.radioDeChip(cgRadio.getCheckedChipId());
                    resolverUbicacionYActualizar(busqueda, categoria, precioMin, precioMax, estadoArticulo,
                            cercania, radioKm);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Si hace falta permiso de ubicación para la cercanía, lo pide y retoma acá cuando el usuario responde. */
    private void resolverUbicacionYActualizar(FavoritoModels.BusquedaGuardadaItem busqueda, String categoria,
                                              Double precioMin, Double precioMax, String estadoArticulo,
                                              boolean cercania, double radioKm) {
        if (cercania && !UbicacionHelper.tienePermiso(this)) {
            pendienteTrasUbicacion = () -> resolverUbicacionYActualizar(busqueda, categoria, precioMin, precioMax,
                    estadoArticulo, cercania, radioKm);
            pedirPermisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
            return;
        }

        Double lat = null;
        Double lng = null;
        if (cercania && UbicacionHelper.tienePermiso(this)) {
            Location ubicacion = UbicacionHelper.ultimaUbicacion(this);
            if (ubicacion == null) {
                Toast.makeText(this, "No pudimos obtener tu ubicación. Activala e intentá de nuevo.",
                        Toast.LENGTH_LONG).show();
            } else {
                lat = ubicacion.getLatitude();
                lng = ubicacion.getLongitude();
            }
        }

        var request = new FavoritoModels.GuardarBusquedaRequest(busqueda.getNombre(), busqueda.getQ(), categoria,
                precioMin, precioMax, estadoArticulo, lat, lng, radioKm);
        FavoritoAcciones.actualizarBusqueda(this, busqueda.getId(), request, actualizada -> {
            int indice = busquedas.indexOf(busqueda);
            if (indice >= 0) {
                busquedas.set(indice, actualizada);
            }
            long conNovedades = busquedas.stream().filter(b -> b.getCantidadNuevas() > 0).count();
            actualizarPestania(TAB_BUSQUEDAS, "Búsquedas guardadas", busquedas.size(), conNovedades);
            mostrarListaActual();
        });
    }

    private void aplicarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
