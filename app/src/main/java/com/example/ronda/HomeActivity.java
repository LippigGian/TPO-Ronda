package com.example.ronda;

import android.Manifest;
import android.location.Location;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Punto 3: explorar publicaciones (listado paginado con búsqueda, filtros combinados y orden). */
public class HomeActivity extends AppCompatActivity {

    private static final int TAMANO_PAGINA = 10;
    private static final int UMBRAL_SCROLL = 3;

    private static final String ORDEN_RECIENTES = "RECIENTES";
    private static final String ORDEN_PRECIO_ASC = "PRECIO_ASC";
    private static final String ORDEN_PRECIO_DESC = "PRECIO_DESC";

    private PublicacionesAdapter adapter;
    private RecyclerView rvPublicaciones;
    private TextView tvSinResultados;
    private ProgressBar progreso;
    private MaterialButton btnOrden;
    private MaterialButton btnFiltros;
    private TextInputEditText etBuscar;
    private TextView tvAvisoOffline;
    private ConnectivityObserver connectivityObserver;
    private PublicacionesCacheManager cache;
    /** true mientras el listado que se ve viene del cache y no del servidor. */
    private boolean mostrandoCache = false;

    private final FiltrosHome filtros = new FiltrosHome();
    private final List<String> categorias = new ArrayList<>();
    private ActivityResultLauncher<String> pedirPermisoUbicacion;
    private Double latitud;
    private Double longitud;

    private String texto;
    private String orden = ORDEN_RECIENTES;

    private int paginaActual = -1;
    private boolean ultimaPagina = false;
    private boolean cargando = false;
    /** Se incrementa con cada búsqueda nueva para descartar respuestas viejas que llegan tarde. */
    private int generacion = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvPublicaciones = findViewById(R.id.rvPublicaciones);
        tvSinResultados = findViewById(R.id.tvSinResultados);
        progreso = findViewById(R.id.progreso);
        btnOrden = findViewById(R.id.btnOrden);
        btnFiltros = findViewById(R.id.btnFiltros);
        etBuscar = findViewById(R.id.etBuscar);
        tvAvisoOffline = findViewById(R.id.tvAvisoOffline);
        cache = PublicacionesCacheManager.getInstance(this);
        connectivityObserver = new ConnectivityObserver(this);

        adapter = new PublicacionesAdapter(this::abrirDetalle);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvPublicaciones.setLayoutManager(layoutManager);
        rvPublicaciones.setAdapter(adapter);
        rvPublicaciones.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int ultimoVisible = layoutManager.findLastVisibleItemPosition();
                if (dy > 0 && ultimoVisible >= adapter.getItemCount() - UMBRAL_SCROLL) {
                    cargarSiguientePagina();
                }
            }
        });

        configurarBusqueda();
        btnOrden.setOnClickListener(this::mostrarMenuOrden);
        btnFiltros.setOnClickListener(v -> mostrarDialogoFiltros());

        pedirPermisoUbicacion = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), concedido -> {
                    if (!concedido) {
                        filtros.cercania = false;
                        Toast.makeText(this, "Sin permiso de ubicación no podemos filtrar por cercanía",
                                Toast.LENGTH_LONG).show();
                    }
                    aplicarFiltros();
                });

        cargarCategorias();
        cargarDesdeCero();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectivityObserver.start(online -> {
            // Al volver la conexión se reemplaza lo guardado por los datos más recientes del servidor.
            if (online && mostrandoCache) {
                runOnUiThread(this::cargarDesdeCero);
            }
        });
    }

    @Override
    protected void onStop() {
        connectivityObserver.stop();
        super.onStop();
    }

    private void configurarBusqueda() {
        TextInputLayout tilBuscar = findViewById(R.id.tilBuscar);
        etBuscar.setOnEditorActionListener((vista, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                buscar();
                return true;
            }
            return false;
        });
        tilBuscar.setEndIconOnClickListener(v -> buscar());
    }

    private void buscar() {
        String ingresado = etBuscar.getText() != null ? etBuscar.getText().toString().trim() : "";
        texto = ingresado.isEmpty() ? null : ingresado;
        cargarDesdeCero();
    }

    private void mostrarMenuOrden(View ancla) {
        PopupMenu menu = new PopupMenu(this, ancla);
        menu.getMenu().add(0, 1, 0, R.string.orden_recientes);
        menu.getMenu().add(0, 2, 1, R.string.orden_precio_asc);
        menu.getMenu().add(0, 3, 2, R.string.orden_precio_desc);
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 2:
                    orden = ORDEN_PRECIO_ASC;
                    btnOrden.setText(R.string.orden_precio_asc);
                    break;
                case 3:
                    orden = ORDEN_PRECIO_DESC;
                    btnOrden.setText(R.string.orden_precio_desc);
                    break;
                default:
                    orden = ORDEN_RECIENTES;
                    btnOrden.setText(R.string.orden_recientes);
                    break;
            }
            cargarDesdeCero();
            return true;
        });
        menu.show();
    }

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
            public void onFailure(Call<List<String>> call, Throwable error) {
                // Sin categorías el filtro queda solo con "Todas"; no es un error para el usuario.
            }
        });
    }

    private void mostrarDialogoFiltros() {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_filtros, null);
        AutoCompleteTextView actvCategoria = vista.findViewById(R.id.actvCategoria);
        TextInputEditText etPrecioMin = vista.findViewById(R.id.etPrecioMin);
        TextInputEditText etPrecioMax = vista.findViewById(R.id.etPrecioMax);
        ChipGroup cgEstado = vista.findViewById(R.id.cgEstado);
        MaterialSwitch swCercania = vista.findViewById(R.id.swCercania);
        ChipGroup cgRadio = vista.findViewById(R.id.cgRadio);

        List<String> opciones = new ArrayList<>();
        opciones.add(getString(R.string.filtro_todas));
        opciones.addAll(categorias);
        actvCategoria.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, opciones));
        actvCategoria.setText(filtros.categoria != null ? filtros.categoria : opciones.get(0), false);

        if (filtros.precioMin != null) etPrecioMin.setText(numeroSinDecimales(filtros.precioMin));
        if (filtros.precioMax != null) etPrecioMax.setText(numeroSinDecimales(filtros.precioMax));
        cgEstado.check(chipDeEstado(filtros.estadoArticulo));

        swCercania.setChecked(filtros.cercania);
        cgRadio.setVisibility(filtros.cercania ? View.VISIBLE : View.GONE);
        cgRadio.check(chipDeRadio(filtros.radioKm));
        swCercania.setOnCheckedChangeListener((boton, marcado) ->
                cgRadio.setVisibility(marcado ? View.VISIBLE : View.GONE));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.filtros_titulo)
                .setView(vista)
                .setPositiveButton(R.string.filtro_aplicar, (dialogo, boton) -> {
                    String elegida = actvCategoria.getText().toString();
                    filtros.categoria = categorias.contains(elegida) ? elegida : null;
                    filtros.precioMin = leerNumero(etPrecioMin);
                    filtros.precioMax = leerNumero(etPrecioMax);
                    if (filtros.precioMin != null && filtros.precioMax != null
                            && filtros.precioMin > filtros.precioMax) {
                        Double intercambio = filtros.precioMin;
                        filtros.precioMin = filtros.precioMax;
                        filtros.precioMax = intercambio;
                    }
                    filtros.estadoArticulo = estadoDeChip(cgEstado.getCheckedChipId());
                    filtros.cercania = swCercania.isChecked();
                    filtros.radioKm = radioDeChip(cgRadio.getCheckedChipId());
                    aplicarFiltros();
                })
                .setNeutralButton(R.string.filtro_limpiar, (dialogo, boton) -> {
                    limpiarFiltros();
                    aplicarFiltros();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Aplica los filtros elegidos; para la cercanía primero se asegura permiso y ubicación. */
    private void aplicarFiltros() {
        latitud = null;
        longitud = null;
        if (filtros.cercania) {
            if (!UbicacionHelper.tienePermiso(this)) {
                pedirPermisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
                return;
            }
            Location ubicacion = UbicacionHelper.ultimaUbicacion(this);
            if (ubicacion == null) {
                filtros.cercania = false;
                Toast.makeText(this, "No pudimos obtener tu ubicación. Activala e intentá de nuevo.",
                        Toast.LENGTH_LONG).show();
            } else {
                latitud = ubicacion.getLatitude();
                longitud = ubicacion.getLongitude();
            }
        }
        actualizarBotonFiltros();
        cargarDesdeCero();
    }

    private void limpiarFiltros() {
        filtros.categoria = null;
        filtros.precioMin = null;
        filtros.precioMax = null;
        filtros.estadoArticulo = null;
        filtros.cercania = false;
        filtros.radioKm = FiltrosHome.RADIO_KM_POR_DEFECTO;
    }

    private void actualizarBotonFiltros() {
        int activos = filtros.cantidadActivos();
        btnFiltros.setText(activos == 0
                ? getString(R.string.home_filtros)
                : getString(R.string.home_filtros) + " (" + activos + ")");
    }

    /** Reinicia el listado (nueva búsqueda, filtro u orden) y trae la primera página. */
    private void cargarDesdeCero() {
        generacion++;
        paginaActual = -1;
        ultimaPagina = false;
        cargando = false;
        adapter.reemplazar(Collections.emptyList());
        tvSinResultados.setVisibility(View.GONE);
        cargarSiguientePagina();
    }

    private void cargarSiguientePagina() {
        if (cargando || ultimaPagina) {
            return;
        }
        int paginaPedida = paginaActual + 1;
        if (paginaPedida == 0 && !ConnectivityObserver.isOnline(this)) {
            mostrarDesdeCache();
            return;
        }
        cargando = true;
        int generacionPedida = generacion;
        progreso.setVisibility(View.VISIBLE);

        boolean conCercania = filtros.cercania && latitud != null && longitud != null;
        ApiClient.api().explorarPublicaciones(
                texto, filtros.categoria, filtros.precioMin, filtros.precioMax, filtros.estadoArticulo,
                latitud, longitud, conCercania ? filtros.radioKm : null,
                orden, paginaPedida, TAMANO_PAGINA
        ).enqueue(new Callback<ApiClient.PageResponse<ApiClient.PublicacionResponse>>() {
            @Override
            public void onResponse(
                    Call<ApiClient.PageResponse<ApiClient.PublicacionResponse>> call,
                    Response<ApiClient.PageResponse<ApiClient.PublicacionResponse>> response
            ) {
                if (generacionPedida != generacion) {
                    return;
                }
                cargando = false;
                progreso.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(
                            HomeActivity.this,
                            ApiClient.errorMessage(response, "No se pudieron cargar las publicaciones"),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }
                mostrarPagina(response.body(), paginaPedida);
            }

            @Override
            public void onFailure(
                    Call<ApiClient.PageResponse<ApiClient.PublicacionResponse>> call,
                    Throwable error
            ) {
                if (generacionPedida != generacion) {
                    return;
                }
                cargando = false;
                progreso.setVisibility(View.GONE);
                if (paginaPedida == 0) {
                    mostrarDesdeCache();
                    return;
                }
                Toast.makeText(HomeActivity.this, "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarPagina(ApiClient.PageResponse<ApiClient.PublicacionResponse> pagina, int numero) {
        List<ApiClient.PublicacionResponse> contenido = pagina.getContent();
        paginaActual = numero;
        ultimaPagina = pagina.isLast();
        ocultarAvisoOffline();

        if (numero == 0) {
            adapter.reemplazar(contenido);
            // Solo se guarda el listado "puro" (sin búsqueda ni filtros) como copia para uso sin conexión.
            if (texto == null && !filtros.hayFiltrosActivos() && ORDEN_RECIENTES.equals(orden)) {
                cache.guardarHome(contenido);
            }
        } else {
            adapter.agregar(contenido);
        }
        tvSinResultados.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    /** Sin conexión: se muestran las últimas publicaciones cargadas con éxito y se avisa que pueden estar viejas. */
    private void mostrarDesdeCache() {
        int generacionPedida = generacion;
        cache.leerHome(guardado -> runOnUiThread(() -> {
            if (generacionPedida != generacion) {
                return;
            }
            progreso.setVisibility(View.GONE);
            mostrandoCache = true;
            ultimaPagina = true;
            paginaActual = 0;
            adapter.reemplazar(guardado.publicaciones);
            tvSinResultados.setVisibility(guardado.publicaciones.isEmpty() ? View.VISIBLE : View.GONE);

            String actualizado = guardado.timestamp > 0
                    ? " Última actualización: " + DateUtils.getRelativeTimeSpanString(guardado.timestamp) + "."
                    : "";
            tvAvisoOffline.setText(getString(R.string.home_aviso_offline) + actualizado);
            tvAvisoOffline.setVisibility(View.VISIBLE);
        }));
    }

    private void ocultarAvisoOffline() {
        mostrandoCache = false;
        tvAvisoOffline.setVisibility(View.GONE);
    }

    private void abrirDetalle(ApiClient.PublicacionResponse publicacion) {
        startActivity(DetallePublicacionActivity.crearIntent(this, publicacion.getId()));
    }

    private static Double leerNumero(TextInputEditText campo) {
        String ingresado = campo.getText() != null ? campo.getText().toString().trim() : "";
        if (ingresado.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(ingresado);
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private static String numeroSinDecimales(double valor) {
        return valor % 1 == 0 ? String.valueOf((long) valor) : String.valueOf(valor);
    }

    private static int chipDeEstado(String estado) {
        if ("NUEVO".equals(estado)) return R.id.chipEstadoNuevo;
        if ("COMO_NUEVO".equals(estado)) return R.id.chipEstadoComoNuevo;
        if ("USADO".equals(estado)) return R.id.chipEstadoUsado;
        return R.id.chipEstadoTodos;
    }

    private static String estadoDeChip(int chipId) {
        if (chipId == R.id.chipEstadoNuevo) return "NUEVO";
        if (chipId == R.id.chipEstadoComoNuevo) return "COMO_NUEVO";
        if (chipId == R.id.chipEstadoUsado) return "USADO";
        return null;
    }

    private static int chipDeRadio(double radioKm) {
        if (radioKm <= 5) return R.id.chipRadio5;
        if (radioKm >= 25) return R.id.chipRadio25;
        return R.id.chipRadio10;
    }

    private static double radioDeChip(int chipId) {
        if (chipId == R.id.chipRadio5) return 5.0;
        if (chipId == R.id.chipRadio25) return 25.0;
        return FiltrosHome.RADIO_KM_POR_DEFECTO;
    }
}
