package com.example.ronda.oferta;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ronda.ApiClient;
import com.example.ronda.ConnectivityObserver;
import com.example.ronda.DetallePublicacionActivity;
import com.example.ronda.R;
import com.example.ronda.util.ApiCallback;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

/**
 * Punto 7: "Mis ofertas". Muestra en un mismo lugar las ofertas enviadas y las recibidas.
 * Para que estén siempre actualizadas se recargan al volver a la pantalla, después de cada
 * acción y cada 15 segundos mientras la pantalla está visible.
 */
public class MisOfertasActivity extends AppCompatActivity implements OfertaItemBinder.Acciones {

    private static final long INTERVALO_ACTUALIZACION_MS = 15_000L;
    private static final int TAB_ENVIADAS = 0;
    private static final int TAB_RECIBIDAS = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable actualizacionPeriodica = new Runnable() {
        @Override
        public void run() {
            cargar();
            handler.postDelayed(this, INTERVALO_ACTUALIZACION_MS);
        }
    };

    private OfertaApi api;
    private TabLayout tabs;
    private LinearLayout contenedor;
    private TextView tvVacio;
    private TextView tvAvisoOffline;
    private ProgressBar progreso;

    private OfertaModels.MisOfertas datos;
    private boolean cargando;
    private boolean online = true;
    private boolean errorYaMostrado;

    public static Intent crearIntent(Context context) {
        return new Intent(context, MisOfertasActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mis_ofertas);
        aplicarInsets();

        api = ApiClient.crearServicio(OfertaApi.class);
        tabs = findViewById(R.id.tabsOfertas);
        contenedor = findViewById(R.id.contenedorOfertas);
        tvVacio = findViewById(R.id.tvSinOfertas);
        tvAvisoOffline = findViewById(R.id.tvAvisoOfflineOfertas);
        progreso = findViewById(R.id.progresoOfertas);

        tabs.addTab(tabs.newTab().setText("Enviadas"));
        tabs.addTab(tabs.newTab().setText("Recibidas"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { mostrarListaActual(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(actualizacionPeriodica); // carga ya y programa las siguientes
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(actualizacionPeriodica);
        super.onPause();
    }

    // ---------- Carga de datos ----------

    private void cargar() {
        online = ConnectivityObserver.isOnline(this);
        tvAvisoOffline.setVisibility(online ? View.GONE : View.VISIBLE);
        if (!online || cargando) {
            mostrarListaActual(); // sin conexión se ve lo último cargado, con botones deshabilitados
            return;
        }

        cargando = true;
        if (datos == null) {
            progreso.setVisibility(View.VISIBLE);
        }
        api.misOfertas().enqueue(new ApiCallback<OfertaModels.MisOfertas>(this, "No se pudieron cargar tus ofertas") {
            @Override
            protected void onExito(OfertaModels.MisOfertas respuesta) {
                datos = respuesta;
                errorYaMostrado = false;
                actualizarPestanias();
                mostrarListaActual();
            }

            @Override
            protected void onError(String mensaje) {
                // La recarga es periódica: se avisa una sola vez para no repetir el mismo Toast.
                if (!errorYaMostrado) {
                    errorYaMostrado = true;
                    super.onError(mensaje);
                }
            }

            @Override
            protected void onFinalizar() {
                cargando = false;
                progreso.setVisibility(View.GONE);
            }
        });
    }

    // ---------- Pantalla ----------

    /** Títulos con cantidad y un badge con las ofertas que esperan mi respuesta. */
    private void actualizarPestanias() {
        actualizarPestania(TAB_ENVIADAS, "Enviadas", datos.getEnviadas());
        actualizarPestania(TAB_RECIBIDAS, "Recibidas", datos.getRecibidas());
    }

    private void actualizarPestania(int posicion, String titulo, List<OfertaModels.Oferta> ofertas) {
        TabLayout.Tab tab = tabs.getTabAt(posicion);
        if (tab == null) {
            return;
        }
        tab.setText(titulo + " (" + ofertas.size() + ")");

        int porResponder = 0;
        for (OfertaModels.Oferta oferta : ofertas) {
            if (oferta.isPuedoResponder()) {
                porResponder++;
            }
        }
        if (porResponder > 0) {
            tab.getOrCreateBadge().setNumber(porResponder);
        } else {
            tab.removeBadge();
        }
    }

    private void mostrarListaActual() {
        contenedor.removeAllViews();
        if (datos == null) {
            return;
        }

        boolean enviadas = tabs.getSelectedTabPosition() != TAB_RECIBIDAS;
        List<OfertaModels.Oferta> ofertas = enviadas ? datos.getEnviadas() : datos.getRecibidas();

        tvVacio.setVisibility(ofertas.isEmpty() ? View.VISIBLE : View.GONE);
        tvVacio.setText(enviadas
                ? "Todavía no hiciste ofertas. Buscá un artículo y tocá \"Ofertar\"."
                : "Todavía no recibiste ofertas por tus publicaciones.");

        LayoutInflater inflater = LayoutInflater.from(this);
        for (OfertaModels.Oferta oferta : ofertas) {
            View fila = inflater.inflate(R.layout.item_oferta, contenedor, false);
            OfertaItemBinder.completar(this, fila, oferta, online, this);
            contenedor.addView(fila);
        }
    }

    // ---------- Acciones de cada fila (OfertaItemBinder.Acciones) ----------

    @Override
    public void aceptar(OfertaModels.Oferta oferta) {
        OfertaAcciones.aceptar(this, oferta, actualizada -> recargarAhora());
    }

    @Override
    public void rechazar(OfertaModels.Oferta oferta) {
        OfertaAcciones.rechazar(this, oferta, actualizada -> recargarAhora());
    }

    @Override
    public void contraofertar(OfertaModels.Oferta oferta) {
        OfertaAcciones.contraofertar(this, oferta, actualizada -> recargarAhora());
    }

    @Override
    public void verPuntoEntrega(OfertaModels.Oferta oferta) {
        OfertaAcciones.verPuntoEntrega(this, oferta);
    }

    @Override
    public void abrirPublicacion(OfertaModels.Oferta oferta) {
        if (!online) {
            Toast.makeText(this, "Necesitás conexión a internet para continuar", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(DetallePublicacionActivity.crearIntent(this, oferta.getPublicacion().getId()));
    }

    /** Después de una acción se recarga enseguida y se reinicia el ciclo de 15 segundos. */
    private void recargarAhora() {
        handler.removeCallbacks(actualizacionPeriodica);
        cargando = false;
        handler.post(actualizacionPeriodica);
    }

    private void aplicarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
