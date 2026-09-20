package com.example.ronda;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.widget.Button;

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
