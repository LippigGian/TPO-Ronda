package com.example.ronda;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.example.ronda.perfil.PerfilPublicoActivity;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Punto 4: detalle de la publicación, con acciones distintas según quién la mire. */
public class DetallePublicacionActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "publicacion_id";

    /** Un solo llamado desde donde se quiera abrir una publicación: startActivity(crearIntent(this, id)). */
    public static Intent crearIntent(Context context, long publicacionId) {
        Intent intent = new Intent(context, DetallePublicacionActivity.class);
        intent.putExtra(EXTRA_ID, publicacionId);
        return intent;
    }

    private long publicacionId;
    private PublicacionesCacheManager cache;
    private ConnectivityObserver connectivityObserver;
    private ApiClient.PublicacionResponse publicacion;
    private boolean mostrandoCache = false;

    private View contenido;
    private ProgressBar progreso;
    private TextView tvAvisoOffline;
    private View accionesInteresado;
    private TextView tvRequiereConexion;
    private MaterialButton btnOfertar;
    private MaterialButton btnPreguntar;
    private MaterialButton btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_publicacion);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        publicacionId = getIntent().getLongExtra(EXTRA_ID, -1);
        if (publicacionId < 0) {
            finish();
            return;
        }

        cache = PublicacionesCacheManager.getInstance(this);
        connectivityObserver = new ConnectivityObserver(this);
        contenido = findViewById(R.id.contenidoDetalle);
        progreso = findViewById(R.id.progresoDetalle);
        tvAvisoOffline = findViewById(R.id.tvAvisoOfflineDetalle);
        accionesInteresado = findViewById(R.id.accionesInteresado);
        tvRequiereConexion = findViewById(R.id.tvRequiereConexion);
        btnOfertar = findViewById(R.id.btnOfertar);
        btnPreguntar = findViewById(R.id.btnPreguntar);
        btnGuardar = findViewById(R.id.btnGuardar);

        cargar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (connectivityObserver == null) {
            return;
        }
        connectivityObserver.start(online -> {
            // Al recuperar la conexión se reemplaza la copia guardada por los datos del servidor.
            if (online && mostrandoCache) {
                runOnUiThread(this::cargar);
            }
        });
    }

    @Override
    protected void onStop() {
        if (connectivityObserver != null) {
            connectivityObserver.stop();
        }
        super.onStop();
    }

    private void cargar() {
        if (!ConnectivityObserver.isOnline(this)) {
            cargarDesdeCache();
            return;
        }
        progreso.setVisibility(View.VISIBLE);
        ApiClient.api().getPublicacion(publicacionId).enqueue(new Callback<ApiClient.PublicacionResponse>() {
            @Override
            public void onResponse(Call<ApiClient.PublicacionResponse> call,
                                   Response<ApiClient.PublicacionResponse> response) {
                progreso.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(DetallePublicacionActivity.this,
                            ApiClient.errorMessage(response, getString(R.string.detalle_no_disponible)),
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                mostrandoCache = false;
                cache.guardarDetalle(response.body());
                mostrar(response.body(), true);
            }

            @Override
            public void onFailure(Call<ApiClient.PublicacionResponse> call, Throwable error) {
                progreso.setVisibility(View.GONE);
                cargarDesdeCache();
            }
        });
    }

    private void cargarDesdeCache() {
        cache.leerDetalle(publicacionId, guardada -> runOnUiThread(() -> {
            progreso.setVisibility(View.GONE);
            if (guardada == null) {
                Toast.makeText(this, "Sin conexión y esta publicación no está guardada en el dispositivo",
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            mostrandoCache = true;
            mostrar(guardada, false);
        }));
    }

    private void mostrar(ApiClient.PublicacionResponse p, boolean online) {
        publicacion = p;
        contenido.setVisibility(View.VISIBLE);
        tvAvisoOffline.setVisibility(online ? View.GONE : View.VISIBLE);

        mostrarGaleria(p.getFotos() != null ? p.getFotos() : Collections.emptyList());

        ((TextView) findViewById(R.id.tvTituloDetalle)).setText(p.getTitulo());
        ((TextView) findViewById(R.id.tvPrecioDetalle)).setText(Formato.precio(p.getPrecio()));
        ((TextView) findViewById(R.id.tvEstadoDetalle)).setText(Formato.estadoArticulo(p.getEstadoArticulo()));
        ((TextView) findViewById(R.id.tvCategoriaDetalle)).setText(p.getCategoria());
        ((TextView) findViewById(R.id.tvDescripcionDetalle)).setText(p.getDescripcion());

        String zona = p.getZona() != null ? p.getZona() : "Zona no informada";
        ((TextView) findViewById(R.id.tvFechaZonaDetalle))
                .setText("Publicado el " + Formato.fecha(p.getCreatedAt()) + " · " + zona);

        mostrarVendedor(p.getVendedor());
        mostrarDireccion(p);
        mostrarAcciones(p, online);
    }

    private void mostrarGaleria(List<String> fotos) {
        ViewPager2 vpFotos = findViewById(R.id.vpFotos);
        TextView tvSinFotos = findViewById(R.id.tvSinFotos);
        TextView tvContador = findViewById(R.id.tvContadorFotos);

        vpFotos.setAdapter(new GaleriaAdapter(fotos));
        tvSinFotos.setVisibility(fotos.isEmpty() ? View.VISIBLE : View.GONE);
        tvContador.setVisibility(fotos.size() > 1 ? View.VISIBLE : View.GONE);
        tvContador.setText(String.format(Locale.getDefault(), "1/%d", fotos.size()));
        vpFotos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tvContador.setText(String.format(Locale.getDefault(), "%d/%d", position + 1, fotos.size()));
            }
        });
    }

    private void mostrarVendedor(ApiClient.VendedorResponse vendedor) {
        TextView nombre = findViewById(R.id.tvNombreVendedor);
        TextView reputacion = findViewById(R.id.tvReputacionVendedor);
        TextView antiguedad = findViewById(R.id.tvAntiguedadVendedor);
        View btnVerPerfil = findViewById(R.id.btnVerPerfil);

        if (vendedor == null) {
            nombre.setText("Vendedor");
            reputacion.setText("");
            antiguedad.setText("");
            btnVerPerfil.setVisibility(View.GONE);
            return;
        }

        nombre.setText(vendedor.getNombre());
        antiguedad.setText("Miembro desde " + Formato.fecha(vendedor.getMiembroDesde()));

        ApiClient.ReputacionResponse r = vendedor.getReputacion();
        if (r == null || r.getPromedioEstrellas() == null) {
            reputacion.setText("Todavía sin calificaciones");
        } else {
            reputacion.setText(String.format(Locale.getDefault(),
                    "★ %.1f (%d calificaciones) · %d ventas · %d compras",
                    r.getPromedioEstrellas(), r.getCantidadCalificaciones(),
                    r.getOperacionesComoVendedor(), r.getOperacionesComoComprador()));
        }

        btnVerPerfil.setVisibility(View.VISIBLE);
        // Punto 2: antes de operar, cualquier persona puede consultar el perfil público del vendedor.
        btnVerPerfil.setOnClickListener(v -> PerfilPublicoActivity.abrir(this, vendedor.getId()));
    }

    /** La dirección exacta solo se ve si el backend la mandó (dueño u oferta aceptada). */
    private void mostrarDireccion(ApiClient.PublicacionResponse p) {
        TextView tvDireccion = findViewById(R.id.tvDireccionDetalle);
        View btnComoLlegar = findViewById(R.id.btnComoLlegarDetalle);

        if (p.isDireccionVisible() && p.getDireccion() != null) {
            tvDireccion.setText(p.getDireccion());
            btnComoLlegar.setVisibility(View.VISIBLE);
            btnComoLlegar.setOnClickListener(v -> MapasHelper.abrirComoLlegar(this, p));
        } else {
            tvDireccion.setText(R.string.detalle_direccion_oculta);
            btnComoLlegar.setVisibility(View.GONE);
        }
    }

    private void mostrarAcciones(ApiClient.PublicacionResponse p, boolean online) {
        View btnGestionar = findViewById(R.id.btnGestionar);

        if (p.isEsPropia()) {
            accionesInteresado.setVisibility(View.GONE);
            btnGestionar.setVisibility(View.VISIBLE);
            btnGestionar.setOnClickListener(v ->
                    startActivity(new Intent(this, MisPublicacionesActivity.class)));
            return;
        }

        btnGestionar.setVisibility(View.GONE);
        accionesInteresado.setVisibility(View.VISIBLE);
        // Sin conexión estas acciones quedan deshabilitadas con un mensaje claro.
        btnOfertar.setEnabled(online);
        btnPreguntar.setEnabled(online);
        btnGuardar.setEnabled(online);
        tvRequiereConexion.setVisibility(online ? View.GONE : View.VISIBLE);

        // Cada acción se conecta con su módulo: ofertas (punto 7) y favoritos (punto 10).
        btnOfertar.setOnClickListener(v -> proximamente("Las ofertas"));
        btnPreguntar.setOnClickListener(v -> proximamente("Las preguntas"));
        btnGuardar.setOnClickListener(v -> proximamente("Los favoritos"));
    }

    private void proximamente(String funcion) {
        Toast.makeText(this, funcion + " se habilitan con su módulo", Toast.LENGTH_SHORT).show();
    }
}
