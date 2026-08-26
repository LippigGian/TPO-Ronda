package com.example.ronda;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.ronda.model.Publicacion;

import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        //Prueba de creacion de objeto Publicacion
        Publicacion publicacion = new Publicacion(
                "Bicicleta mountain bike",
                250000,
                "Usado",
                "Palermo"
        );
//       Buscar los datos del componente:

        TextView tvTitulo = findViewById(R.id.tvTituloPublicacion);
        TextView tvPrecio = findViewById(R.id.tvPrecioPublicacion);
        TextView tvEstado = findViewById(R.id.tvEstadoPublicacion);
        TextView tvZona = findViewById(R.id.tvZonaPublicacion);
//      Mostrar los datos:
        tvTitulo.setText(publicacion.getTitulo());
        tvPrecio.setText("$ " + publicacion.getPrecio());
        tvEstado.setText("Estado: " + publicacion.getEstado());
        tvZona.setText("Zona: " + publicacion.getZona());


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}