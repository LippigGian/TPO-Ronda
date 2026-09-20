package com.example.ronda;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
        setContentView(R.layout.activity_punto_entrega);

        String titulo = getIntent().getStringExtra(EXTRA_TITULO);
        String direccion = getIntent().getStringExtra(EXTRA_DIRECCION);
        double latitud = getIntent().getDoubleExtra(EXTRA_LATITUD, 0.0);
        double longitud = getIntent().getDoubleExtra(EXTRA_LONGITUD, 0.0);

        ((TextView) findViewById(R.id.tvTituloEntrega)).setText(titulo != null ? titulo : "");
        ((TextView) findViewById(R.id.tvDireccionEntrega))
                .setText(direccion != null ? direccion : "Sin dirección definida");

        Button btnComoLlegar = findViewById(R.id.btnComoLlegar);
        btnComoLlegar.setOnClickListener(v ->
                MapasHelper.abrirComoLlegar(this, direccion, latitud, longitud));
    }
}
