package com.example.ronda;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

/**
 * Punto 8: una vez aceptada la oferta, el comprador ve el punto de entrega y lo abre con
 * "Cómo llegar" en Google Maps (o la app de mapas del dispositivo) en modo navegación.
 * No necesita conexión: los datos llegan con la oferta aceptada.
 */
public class PuntoEntregaActivity extends AppCompatActivity {

    public static final String EXTRA_TITULO = "titulo";
    public static final String EXTRA_DIRECCION = "direccion";
    public static final String EXTRA_LATITUD = "latitud";
    public static final String EXTRA_LONGITUD = "longitud";

    /** Un solo llamado desde donde se acepta la oferta: startActivity(PuntoEntregaActivity.crearIntent(this, publicacion)); */
    public static Intent crearIntent(Context context, ApiClient.PublicacionResponse publicacion) {
        Intent intent = new Intent(context, PuntoEntregaActivity.class);
        intent.putExtra(EXTRA_TITULO, publicacion.getTitulo());
        intent.putExtra(EXTRA_DIRECCION, publicacion.getDireccion());
        intent.putExtra(EXTRA_LATITUD, publicacion.getLatitud());
        intent.putExtra(EXTRA_LONGITUD, publicacion.getLongitud());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_punto_entrega);
        // Con edge-to-edge el contenido no queda debajo de la barra de estado ni de la de navegación.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        String titulo = getIntent().getStringExtra(EXTRA_TITULO);
        String direccion = getIntent().getStringExtra(EXTRA_DIRECCION);
        double latitud = getIntent().getDoubleExtra(EXTRA_LATITUD, 0.0);
        double longitud = getIntent().getDoubleExtra(EXTRA_LONGITUD, 0.0);
        boolean tieneDireccion = direccion != null && !direccion.trim().isEmpty();
        boolean tieneCoordenadas = latitud != 0.0 || longitud != 0.0;

        ((TextView) findViewById(R.id.tvTituloEntrega)).setText(titulo != null ? titulo : "");
        ((TextView) findViewById(R.id.tvDireccionEntrega))
                .setText(tieneDireccion ? direccion : getString(R.string.entrega_sin_direccion));

        TextView tvCoordenadas = findViewById(R.id.tvCoordenadasEntrega);
        if (tieneCoordenadas) {
            tvCoordenadas.setText(String.format(Locale.US, "%.5f, %.5f", latitud, longitud));
            tvCoordenadas.setVisibility(View.VISIBLE);
        }

        Button btnComoLlegar = findViewById(R.id.btnComoLlegar);
        btnComoLlegar.setEnabled(tieneDireccion || tieneCoordenadas);
        btnComoLlegar.setOnClickListener(v ->
                MapasHelper.abrirComoLlegar(this, direccion, latitud, longitud));
    }
}
