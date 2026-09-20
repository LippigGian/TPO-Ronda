package com.example.ronda;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalificarActivity extends AppCompatActivity {
    private long operacionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calificar);

        operacionId = getIntent().getLongExtra("OPERACION_ID", -1);
        String contraparteNombre = getIntent().getStringExtra("CONTRAPARTE_NOMBRE");
        String publicacionTitulo = getIntent().getStringExtra("PUBLICACION_TITULO");

        if (operacionId < 0) {
            finish();
            return;
        }

        TextView tvContraparte = findViewById(R.id.tvContraparteCalificar);
        tvContraparte.setText("Operación con " + contraparteNombre + " · \"" + publicacionTitulo + "\"");

        RatingBar ratingBar = findViewById(R.id.ratingBarCalificacion);
        EditText etComentario = findViewById(R.id.etComentarioCalificacion);
        MaterialButton btnConfirmar = findViewById(R.id.btnConfirmarCalificacion);

        btnConfirmar.setOnClickListener(view -> {
            int puntaje = Math.round(ratingBar.getRating());
            if (puntaje < 1) {
                Toast.makeText(this, "Seleccioná al menos una estrella", Toast.LENGTH_LONG).show();
                return;
            }

            String comentario = etComentario.getText().toString().trim();
            btnConfirmar.setEnabled(false);

            ApiClient.api()
                    .crearCalificacion(operacionId, new ApiClient.CalificacionRequest(
                            puntaje, comentario.isEmpty() ? null : comentario))
                    .enqueue(new Callback<ApiClient.CalificacionResponse>() {
                        @Override
                        public void onResponse(
                                Call<ApiClient.CalificacionResponse> call,
                                Response<ApiClient.CalificacionResponse> response
                        ) {
                            if (response.isSuccessful()) {
                                Toast.makeText(CalificarActivity.this,
                                        "¡Gracias por calificar!", Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }
                            btnConfirmar.setEnabled(true);
                            Toast.makeText(CalificarActivity.this,
                                    ApiClient.errorMessage(response, "No se pudo enviar la calificación"),
                                    Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(Call<ApiClient.CalificacionResponse> call, Throwable error) {
                            btnConfirmar.setEnabled(true);
                            Toast.makeText(CalificarActivity.this,
                                    "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show();
                        }
                    });
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
