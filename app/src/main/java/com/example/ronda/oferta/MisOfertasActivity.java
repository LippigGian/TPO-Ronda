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
import android.widget.ScrollView;
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
import com.google.android.material.card.MaterialCardView;
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
    /** Punto 11: oferta a mostrar al abrir la pantalla desde una notificación. */
    private static final String EXTRA_OFERTA_ID = "oferta_id";

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
    /** Oferta que llegó por una notificación: se abre su pestaña y se resalta. -1 si no hay. */
    private long ofertaDestacada = -1;
    /** Se hace scroll hasta la oferta destacada una sola vez; las recargas periódicas no lo repiten. */
    private boolean scrollPendiente;

    public static Intent crearIntent(Context context) {
        return new Intent(context, MisOfertasActivity.class);
    }

    /** Punto 11: al tocar un aviso de oferta se abre "Mis ofertas" directamente en esa oferta. */
    public static Intent crearIntent(Context context, long ofertaId) {
        return crearIntent(context).putExtra(EXTRA_OFERTA_ID, ofertaId);
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
        ofertaDestacada = getIntent().getLongExtra(EXTRA_OFERTA_ID, -1);
        scrollPendiente = ofertaDestacada != -1;

        tabs.addTab(tabs.newTab().setText("Enviadas"));
        tabs.addTab(tabs.newTab().setText("Recibidas"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { mostrarListaActual(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    /** Con la pantalla ya abierta, otra notificación de oferta llega por acá (SINGLE_TOP). */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        ofertaDestacada = intent.getLongExtra(EXTRA_OFERTA_ID, -1);
        scrollPendiente = ofertaDestacada != -1;
        recargarAhora();
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
                seleccionarPestaniaDeOfertaDestacada();
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
            if (oferta.getId() == ofertaDestacada) {
                resaltar(fila);
            }
            contenedor.addView(fila);
        }
        if (scrollPendiente && ofertaDestacada != -1) {
            // Se espera a que las tarjetas estén medidas para conocer la posición de la destacada.
            contenedor.postDelayed(this::scrollHastaDestacada, 250);
        }
    }

    // ---------- Oferta abierta desde una notificación (punto 11) ----------

    /** Selecciona la pestaña donde está la oferta del aviso: enviadas si soy comprador, recibidas si vendo. */
    private void seleccionarPestaniaDeOfertaDestacada() {
        if (ofertaDestacada == -1) {
            return;
        }
        int pestania = contiene(datos.getRecibidas(), ofertaDestacada) ? TAB_RECIBIDAS
                : contiene(datos.getEnviadas(), ofertaDestacada) ? TAB_ENVIADAS : -1;
        if (pestania == -1) {
            ofertaDestacada = -1;
            Toast.makeText(this, "La oferta ya no está disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        TabLayout.Tab tab = tabs.getTabAt(pestania);
        if (tab != null && tabs.getSelectedTabPosition() != pestania) {
            tab.select(); // dispara mostrarListaActual()
        }
    }

    private static boolean contiene(List<OfertaModels.Oferta> ofertas, long ofertaId) {
        for (OfertaModels.Oferta oferta : ofertas) {
            if (oferta.getId() == ofertaId) {
                return true;
            }
        }
        return false;
    }

    /** Borde de color en la tarjeta de la oferta del aviso. */
    private void resaltar(View fila) {
        fila.setTag(ofertaDestacada);
        if (fila instanceof MaterialCardView) {
            MaterialCardView tarjeta = (MaterialCardView) fila;
            tarjeta.setStrokeColor(getColor(R.color.ronda_primary));
            tarjeta.setStrokeWidth(Math.round(3 * getResources().getDisplayMetrics().density));
        }
    }

    private void scrollHastaDestacada() {
        if (!scrollPendiente || !(contenedor.getParent() instanceof ScrollView)) {
            return;
        }
        for (int i = 0; i < contenedor.getChildCount(); i++) {
            View fila = contenedor.getChildAt(i);
            if (Long.valueOf(ofertaDestacada).equals(fila.getTag())) {
                scrollPendiente = false;
                ((ScrollView) contenedor.getParent()).smoothScrollTo(0, fila.getTop());
                return;
            }
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
    public void confirmarRecepcion(OfertaModels.Oferta oferta) {
        OfertaAcciones.confirmarRecepcion(this, oferta);
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
