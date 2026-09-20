package com.example.ronda;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Punto 3: explorar publicaciones (listado paginado con búsqueda y orden). */
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
    private TextInputEditText etBuscar;

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
        etBuscar = findViewById(R.id.etBuscar);

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

        cargarDesdeCero();
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

    /** Reinicia el listado (nueva búsqueda, filtro u orden) y trae la primera página. */
    private void cargarDesdeCero() {
        generacion++;
        paginaActual = -1;
        ultimaPagina = false;
        cargando = false;
        adapter.reemplazar(java.util.Collections.emptyList());
        tvSinResultados.setVisibility(View.GONE);
        cargarSiguientePagina();
    }

    private void cargarSiguientePagina() {
        if (cargando || ultimaPagina) {
            return;
        }
        cargando = true;
        int paginaPedida = paginaActual + 1;
        int generacionPedida = generacion;
        progreso.setVisibility(View.VISIBLE);

        ApiClient.api().explorarPublicaciones(
                texto, null, null, null, null, null, null, null,
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
                Toast.makeText(HomeActivity.this, "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarPagina(ApiClient.PageResponse<ApiClient.PublicacionResponse> pagina, int numero) {
        List<ApiClient.PublicacionResponse> contenido = pagina.getContent();
        paginaActual = numero;
        ultimaPagina = pagina.isLast();

        if (numero == 0) {
            adapter.reemplazar(contenido);
        } else {
            adapter.agregar(contenido);
        }
        tvSinResultados.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void abrirDetalle(ApiClient.PublicacionResponse publicacion) {
        // El detalle de la publicación se conecta en el siguiente PR (punto 4).
        Toast.makeText(this, publicacion.getTitulo(), Toast.LENGTH_SHORT).show();
    }
}
