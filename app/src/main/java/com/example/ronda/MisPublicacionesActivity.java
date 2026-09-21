package com.example.ronda;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.bumptech.glide.Glide;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MisPublicacionesActivity extends AppCompatActivity {

    private LinearLayout contenedor;
    private TextView tvSinPublicaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mis_publicaciones);

        contenedor = findViewById(R.id.contenedorMisPublicaciones);
        tvSinPublicaciones = findViewById(R.id.tvSinPublicaciones);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarPublicaciones();
    }

    private void cargarPublicaciones() {
        ApiClient.api().getMisPublicaciones()
                .enqueue(new Callback<List<ApiClient.PublicacionResponse>>() {
                    @Override
                    public void onResponse(
                            Call<List<ApiClient.PublicacionResponse>> call,
                            Response<List<ApiClient.PublicacionResponse>> response
                    ) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(
                                    MisPublicacionesActivity.this,
                                    ApiClient.errorMessage(
                                            response,
                                            "No se pudieron cargar tus publicaciones"
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        mostrarPublicaciones(response.body());
                    }

                    @Override
                    public void onFailure(
                            Call<List<ApiClient.PublicacionResponse>> call,
                            Throwable error
                    ) {
                        Toast.makeText(
                                MisPublicacionesActivity.this,
                                "No se pudo conectar con el servidor",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void mostrarPublicaciones(List<ApiClient.PublicacionResponse> publicaciones) {
        contenedor.removeAllViews();

        if (publicaciones.isEmpty()) {
            tvSinPublicaciones.setVisibility(View.VISIBLE);
            return;
        }

        tvSinPublicaciones.setVisibility(View.GONE);

        for (ApiClient.PublicacionResponse publicacion : publicaciones) {
            contenedor.addView(crearTarjeta(publicacion));
        }
    }

    private MaterialCardView crearTarjeta(ApiClient.PublicacionResponse publicacion) {
        MaterialCardView tarjeta = new MaterialCardView(this);
        tarjeta.setCardBackgroundColor(getColor(R.color.ronda_surface));
        tarjeta.setRadius(dp(20));
        tarjeta.setCardElevation(dp(2));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        tarjeta.setLayoutParams(cardParams);

        LinearLayout contenido = new LinearLayout(this);
        contenido.setOrientation(LinearLayout.VERTICAL);
        contenido.setPadding(dp(20), dp(20), dp(20), dp(16));
        tarjeta.addView(contenido);

        if (publicacion.getFotos() != null && !publicacion.getFotos().isEmpty()) {
            ImageView imagen = new ImageView(this);
            imagen.setScaleType(ImageView.ScaleType.CENTER_CROP);

            LinearLayout.LayoutParams imagenParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(180)
            );
            imagenParams.bottomMargin = dp(16);
            imagen.setLayoutParams(imagenParams);
            contenido.addView(imagen);

            Glide.with(this)
                    .load(ApiClient.imageUrl(publicacion.getFotos().get(0)))
                    .centerCrop()
                    .into(imagen);
        }

        LinearLayout encabezado = new LinearLayout(this);
        encabezado.setGravity(android.view.Gravity.CENTER_VERTICAL);
        contenido.addView(encabezado);

        TextView titulo = new TextView(this);
        titulo.setText(publicacion.getTitulo());
        titulo.setTextColor(getColor(R.color.ronda_text_primary));
        titulo.setTextSize(20);
        titulo.setTypeface(null, 1);

        LinearLayout.LayoutParams tituloParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        titulo.setLayoutParams(tituloParams);
        encabezado.addView(titulo);

        MaterialButton btnEliminar = new MaterialButton(this);
        btnEliminar.setText("");
        btnEliminar.setAllCaps(false);
        btnEliminar.setContentDescription("Eliminar publicación");
        btnEliminar.setIconResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnEliminar.setIconTint(ColorStateList.valueOf(getColor(android.R.color.holo_red_dark)));
        btnEliminar.setIconSize(dp(24));
        btnEliminar.setBackgroundTintList(
                ColorStateList.valueOf(getColor(android.R.color.transparent))
        );
        btnEliminar.setOnClickListener(view -> confirmarEliminacion(publicacion));
        encabezado.addView(btnEliminar, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView precio = new TextView(this);
        precio.setText("$ " + publicacion.getPrecio());
        precio.setTextColor(getColor(R.color.ronda_primary));
        precio.setTextSize(18);
        precio.setTypeface(null, 1);

        LinearLayout.LayoutParams precioParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        precioParams.topMargin = dp(8);
        precio.setLayoutParams(precioParams);
        contenido.addView(precio);

        TextView detalles = new TextView(this);
        detalles.setText(
                "Categoría: " + publicacion.getCategoria()
                        + "\nArtículo: " + publicacion.getEstadoArticulo()
                        + "\nPublicación: " + publicacion.getEstadoPublicacion()
        );
        detalles.setTextColor(getColor(R.color.ronda_text_secondary));
        detalles.setTextSize(14);

        LinearLayout.LayoutParams detallesParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detallesParams.topMargin = dp(12);
        detalles.setLayoutParams(detallesParams);
        contenido.addView(detalles);

        if (!publicacion.getEstadoPublicacion().equals("VENDIDA")) {
            boolean activa = publicacion.getEstadoPublicacion().equals("ACTIVA");
            MaterialButton btnCambiarEstado = new MaterialButton(this);

            btnCambiarEstado.setText(
                    activa ? "Pausar publicación" : "Reactivar publicación"
            );
            btnCambiarEstado.setAllCaps(false);
            btnCambiarEstado.setTextColor(getColor(R.color.ronda_primary));
            btnCambiarEstado.setBackgroundTintList(
                    ColorStateList.valueOf(getColor(android.R.color.transparent))
            );
            btnCambiarEstado.setStrokeColor(
                    ColorStateList.valueOf(getColor(R.color.ronda_outline))
            );
            btnCambiarEstado.setStrokeWidth(dp(1));
            btnCambiarEstado.setCornerRadius(dp(14));

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
            );
            buttonParams.topMargin = dp(16);
            btnCambiarEstado.setLayoutParams(buttonParams);

            String nuevoEstado = activa ? "PAUSADA" : "ACTIVA";

            btnCambiarEstado.setOnClickListener(view ->
                    cambiarEstado(
                            publicacion.getId(),
                            nuevoEstado,
                            btnCambiarEstado
                    )
            );

            contenido.addView(btnCambiarEstado);

            MaterialButton btnMarcarVendida = new MaterialButton(this);
            btnMarcarVendida.setText("Marcar como vendida");
            btnMarcarVendida.setAllCaps(false);
            btnMarcarVendida.setTextColor(getColor(android.R.color.white));
            btnMarcarVendida.setBackgroundTintList(
                    ColorStateList.valueOf(getColor(R.color.ronda_primary))
            );
            btnMarcarVendida.setCornerRadius(dp(14));

            LinearLayout.LayoutParams vendidaParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
            );
            vendidaParams.topMargin = dp(10);
            btnMarcarVendida.setLayoutParams(vendidaParams);

            btnMarcarVendida.setOnClickListener(view -> mostrarDialogoVender(publicacion.getId()));

            contenido.addView(btnMarcarVendida);
        }

        return tarjeta;
    }

    private void mostrarDialogoVender(long publicacionId) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_vender_publicacion, null);
        com.google.android.material.textfield.TextInputEditText etCompradorEmail =
                dialogView.findViewById(R.id.etCompradorEmail);
        com.google.android.material.textfield.TextInputEditText etMontoFinal =
                dialogView.findViewById(R.id.etMontoFinal);

        new AlertDialog.Builder(this)
                .setTitle("Marcar como vendida")
                .setMessage("Ingresá el email del comprador y el monto final acordado.")
                .setView(dialogView)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar venta", (dialog, which) -> {
                    String compradorEmail = etCompradorEmail.getText().toString().trim();
                    String montoTexto = etMontoFinal.getText().toString().trim();

                    if (compradorEmail.isEmpty() || montoTexto.isEmpty()) {
                        Toast.makeText(this, "Completá el email y el monto", Toast.LENGTH_LONG).show();
                        return;
                    }

                    double montoFinal;
                    try {
                        montoFinal = Double.parseDouble(montoTexto.replace(",", "."));
                        if (montoFinal <= 0) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException error) {
                        Toast.makeText(this, "Ingresá un monto válido", Toast.LENGTH_LONG).show();
                        return;
                    }

                    venderPublicacion(publicacionId, compradorEmail, montoFinal);
                })
                .show();
    }

    private void venderPublicacion(long publicacionId, String compradorEmail, double montoFinal) {
        ApiClient.api()
                .venderPublicacion(publicacionId, new ApiClient.VenderRequest(compradorEmail, montoFinal))
                .enqueue(new Callback<ApiClient.HistorialItemResponse>() {
                    @Override
                    public void onResponse(
                            Call<ApiClient.HistorialItemResponse> call,
                            Response<ApiClient.HistorialItemResponse> response
                    ) {
                        if (response.isSuccessful()) {
                            Toast.makeText(
                                    MisPublicacionesActivity.this,
                                    "Publicación marcada como vendida",
                                    Toast.LENGTH_LONG
                            ).show();
                            cargarPublicaciones();
                            return;
                        }
                        Toast.makeText(
                                MisPublicacionesActivity.this,
                                ApiClient.errorMessage(response, "No se pudo registrar la venta"),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onFailure(Call<ApiClient.HistorialItemResponse> call, Throwable error) {
                        Toast.makeText(
                                MisPublicacionesActivity.this,
                                "No se pudo conectar con el servidor",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void cambiarEstado(
            long id,
            String nuevoEstado,
            MaterialButton boton
    ) {
        boton.setEnabled(false);

        ApiClient.api()
                .changePublicacionStatus(
                        id,
                        new ApiClient.ChangePublicacionStatusRequest(nuevoEstado)
                )
                .enqueue(new Callback<ApiClient.PublicacionResponse>() {
                    @Override
                    public void onResponse(
                            Call<ApiClient.PublicacionResponse> call,
                            Response<ApiClient.PublicacionResponse> response
                    ) {
                        boton.setEnabled(true);

                        if (response.isSuccessful()) {
                            cargarPublicaciones();
                            return;
                        }

                        Toast.makeText(
                                MisPublicacionesActivity.this,
                                ApiClient.errorMessage(
                                        response,
                                        "No se pudo cambiar el estado"
                                ),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onFailure(
                            Call<ApiClient.PublicacionResponse> call,
                            Throwable error
                    ) {
                        boton.setEnabled(true);

                        Toast.makeText(
                                MisPublicacionesActivity.this,
                                "No se pudo conectar con el servidor",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void confirmarEliminacion(ApiClient.PublicacionResponse publicacion) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar publicación")
                .setMessage("¿Querés eliminar \"" + publicacion.getTitulo()
                        + "\"? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarPublicacion(publicacion.getId()))
                .show();
    }

    private void eliminarPublicacion(long id) {
        ApiClient.api().deletePublicacion(id)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.code() == 204) {
                            cargarPublicaciones();
                            return;
                        }

                        Toast.makeText(
                                MisPublicacionesActivity.this,
                                ApiClient.errorMessage(response, "No se pudo eliminar la publicación"),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable error) {
                        Toast.makeText(
                                MisPublicacionesActivity.this,
                                "No se pudo conectar con el servidor",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
