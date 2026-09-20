package com.example.ronda;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

import com.example.ronda.favorito.MisFavoritosActivity;
import com.example.ronda.oferta.MisOfertasActivity;
import com.example.ronda.perfil.PerfilActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        /** Botones de las publicaciones **/
        Button btnIrAExplorar = findViewById(R.id.btnIrAExplorar);
        Button btnIrAPublicarArticulo = findViewById(R.id.btnIrAPublicarArticulo);
        Button btnIrAMisPublicaciones = findViewById(R.id.btnIrAMisPublicaciones);
        Button btnIrAHistorial = findViewById(R.id.btnIrAHistorial);
        Button btnIrAMiPerfil = findViewById(R.id.btnIrAMiPerfil);
        Button btnIrAMisOfertas = findViewById(R.id.btnIrAMisOfertas);
        Button btnIrAMisFavoritos = findViewById(R.id.btnIrAMisFavoritos);


        /** Listeners de publicaciones **/
        btnIrAExplorar.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, HomeActivity.class))
        );

        btnIrAPublicarArticulo.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, PublicarArticuloActivity.class))
        );

        btnIrAMisPublicaciones.setOnClickListener(view ->
                startActivity(
                        new Intent(MainActivity.this, MisPublicacionesActivity.class)
                )
        );

        btnIrAMiPerfil.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, PerfilActivity.class))
        );

        btnIrAMisOfertas.setOnClickListener(view ->
                startActivity(MisOfertasActivity.crearIntent(MainActivity.this))
        );

        btnIrAMisFavoritos.setOnClickListener(view ->
                startActivity(MisFavoritosActivity.crearIntent(MainActivity.this))
        );

        btnIrAHistorial.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, HistorialOperacionesActivity.class))
        );


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
