package com.example.ronda;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PerfilUsuarioActivity extends AppCompatActivity {
    private long usuarioId;
    private LinearLayout contenedor;
    private TextView tvSinCalificaciones;
    private TextView tvNombre;
    private TextView tvReputacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil_usuario);

        usuarioId = getIntent().getLongExtra("USUARIO_ID", -1);
        if (usuarioId < 0) {
            finish();
            return;
        }

        tvNombre = findViewById(R.id.tvNombreUsuarioPerfil);
        tvReputacion = findViewById(R.id.tvReputacionPerfil);
        contenedor = findViewById(R.id.contenedorCalificaciones);
        tvSinCalificaciones = findViewById(R.id.tvSinCalificaciones);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarReputacion();
    }

    private void cargarReputacion() {
        ApiClient.api().getReputacion(usuarioId)
                .enqueue(new Callback<ApiClient.ReputacionResponse>() {
                    @Override
                    public void onResponse(
                            Call<ApiClient.ReputacionResponse> call,
                            Response<ApiClient.ReputacionResponse> response
                    ) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(PerfilUsuarioActivity.this,
                                    ApiClient.errorMessage(response, "No se pudo cargar el perfil"),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        mostrarReputacion(response.body());
                    }

                    @Override
                    public void onFailure(Call<ApiClient.ReputacionResponse> call, Throwable error) {
                        Toast.makeText(PerfilUsuarioActivity.this,
                                "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void mostrarReputacion(ApiClient.ReputacionResponse reputacion) {
        tvNombre.setText(reputacion.getNombreUsuario());
        tvReputacion.setText(String.format(Locale.getDefault(), "⭐ %.1f (%d calificaciones)",
                reputacion.getPromedio(), reputacion.getCantidadCalificaciones()));

        contenedor.removeAllViews();
        List<ApiClient.CalificacionResponse> calificaciones = reputacion.getUltimasCalificaciones();

        if (calificaciones == null || calificaciones.isEmpty()) {
            tvSinCalificaciones.setVisibility(View.VISIBLE);
            return;
        }
        tvSinCalificaciones.setVisibility(View.GONE);

        for (ApiClient.CalificacionResponse calificacion : calificaciones) {
            contenedor.addView(crearTarjeta(calificacion));
        }
    }

    private MaterialCardView crearTarjeta(ApiClient.CalificacionResponse calificacion) {
        MaterialCardView tarjeta = new MaterialCardView(this);
        tarjeta.setCardBackgroundColor(getColor(R.color.ronda_surface));
        tarjeta.setRadius(dp(16));
        tarjeta.setCardElevation(dp(1));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        tarjeta.setLayoutParams(cardParams);

        LinearLayout contenido = new LinearLayout(this);
        contenido.setOrientation(LinearLayout.VERTICAL);
        contenido.setPadding(dp(16), dp(12), dp(16), dp(12));
        tarjeta.addView(contenido);

        StringBuilder estrellas = new StringBuilder();
        for (int i = 0; i < calificacion.getPuntaje(); i++) {
            estrellas.append("★");
        }

        TextView autorYPuntaje = new TextView(this);
        autorYPuntaje.setText(calificacion.getAutorNombre() + " · " + estrellas);
        autorYPuntaje.setTextColor(getColor(R.color.ronda_text_primary));
        autorYPuntaje.setTextSize(15);
        autorYPuntaje.setTypeface(null, android.graphics.Typeface.BOLD);
        contenido.addView(autorYPuntaje);

        if (calificacion.getComentario() != null && !calificacion.getComentario().isEmpty()) {
            TextView comentario = new TextView(this);
            comentario.setText(calificacion.getComentario());
            comentario.setTextColor(getColor(R.color.ronda_text_secondary));
            comentario.setTextSize(14);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(6);
            comentario.setLayoutParams(params);
            contenido.addView(comentario);
        }

        return tarjeta;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
