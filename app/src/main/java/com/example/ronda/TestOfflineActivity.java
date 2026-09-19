package com.example.ronda;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class TestOfflineActivity extends AppCompatActivity {

    private static final String JSON_FAKE = "[{"
            + "\"id\":1,"
            + "\"titulo\":\"Bicicleta rodado 26\","
            + "\"descripcion\":\"Poco uso, service reciente\","
            + "\"categoria\":\"Deportes\","
            + "\"precio\":45000.0,"
            + "\"estadoArticulo\":\"usado\","
            + "\"estadoPublicacion\":\"activa\","
            + "\"direccion\":\"Palermo, CABA\","
            + "\"latitud\":-34.58,"
            + "\"longitud\":-58.43,"
            + "\"fotos\":[]"
            + "}]";

    private ConnectivityObserver connectivityObserver;
    private TextView tvOnline;
    private TextView tvResultado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_offline);

        tvOnline = findViewById(R.id.tvOnline);
        tvResultado = findViewById(R.id.tvResultado);
        connectivityObserver = new ConnectivityObserver(this);

        Button btnProbarConexion = findViewById(R.id.btnProbarConexion);
        Button btnGuardarHomeFake = findViewById(R.id.btnGuardarHomeFake);
        Button btnLeerHome = findViewById(R.id.btnLeerHome);
        Button btnGuardarDetalleFake = findViewById(R.id.btnGuardarDetalleFake);
        Button btnLeerDetalle = findViewById(R.id.btnLeerDetalle);

        btnProbarConexion.setOnClickListener(v -> {
            boolean online = ConnectivityObserver.isOnline(this);
            tvOnline.setText("Estado: " + (online ? "ONLINE" : "OFFLINE"));
        });

        btnGuardarHomeFake.setOnClickListener(v -> {
            List<ApiClient.PublicacionResponse> lista = parsearFake();
            PublicacionesCacheManager.getInstance(this).guardarHome(lista);
            tvResultado.setText("Home fake guardado (" + lista.size() + " publicaciones)");
        });

        btnLeerHome.setOnClickListener(v ->
                PublicacionesCacheManager.getInstance(this).leerHome(cache ->
                        runOnUiThread(() -> tvResultado.setText(
                                "Home leído: " + cache.publicaciones.size() + " publicaciones, timestamp="
                                        + cache.timestamp))));

        btnGuardarDetalleFake.setOnClickListener(v -> {
            List<ApiClient.PublicacionResponse> lista = parsearFake();
            PublicacionesCacheManager.getInstance(this).guardarDetalle(lista.get(0));
            tvResultado.setText("Detalle fake guardado (id=1)");
        });

        btnLeerDetalle.setOnClickListener(v ->
                PublicacionesCacheManager.getInstance(this).leerDetalle(1L, publicacion ->
                        runOnUiThread(() -> tvResultado.setText(
                                publicacion != null
                                        ? "Detalle leído: " + publicacion.getTitulo() + " - $" + publicacion.getPrecio()
                                        : "No se encontró el detalle en cache"))));
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectivityObserver.start(online ->
                runOnUiThread(() -> tvOnline.setText("Estado (listener): " + (online ? "ONLINE" : "OFFLINE"))));
    }

    @Override
    protected void onStop() {
        super.onStop();
        connectivityObserver.stop();
    }

    private List<ApiClient.PublicacionResponse> parsearFake() {
        Type tipoLista = new TypeToken<List<ApiClient.PublicacionResponse>>() {}.getType();
        return new Gson().fromJson(JSON_FAKE, tipoLista);
    }
}
