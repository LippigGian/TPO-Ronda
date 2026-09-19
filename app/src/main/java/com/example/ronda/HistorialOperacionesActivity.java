package com.example.ronda;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistorialOperacionesActivity extends AppCompatActivity {
    private static final String[] TIPOS_API = {"TODAS", "COMPRA", "VENTA"};

    private LinearLayout contenedor;
    private TextView tvSinOperaciones;
    private Spinner spinnerTipo;
    private MaterialButton btnFiltroDesde;
    private MaterialButton btnFiltroHasta;

    private Integer anioDesde, mesDesde, diaDesde;
    private Integer anioHasta, mesHasta, diaHasta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historial_operaciones);

        contenedor = findViewById(R.id.contenedorHistorial);
        tvSinOperaciones = findViewById(R.id.tvSinOperaciones);
        spinnerTipo = findViewById(R.id.spinnerTipoOperacion);
        btnFiltroDesde = findViewById(R.id.btnFiltroDesde);
        btnFiltroHasta = findViewById(R.id.btnFiltroHasta);
        TextView tvLimpiarFiltros = findViewById(R.id.tvLimpiarFiltros);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Todas", "Compras", "Ventas"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adapter);
        spinnerTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cargarHistorial();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        btnFiltroDesde.setOnClickListener(view -> elegirFecha(true));
        btnFiltroHasta.setOnClickListener(view -> elegirFecha(false));

        tvLimpiarFiltros.setOnClickListener(view -> {
            anioDesde = mesDesde = diaDesde = null;
            anioHasta = mesHasta = diaHasta = null;
            btnFiltroDesde.setText("Desde");
            btnFiltroHasta.setText("Hasta");
            cargarHistorial();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarHistorial();
    }

    private void elegirFecha(boolean esDesde) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String textoBoton = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
            if (esDesde) {
                anioDesde = year;
                mesDesde = month;
                diaDesde = dayOfMonth;
                btnFiltroDesde.setText(textoBoton);
            } else {
                anioHasta = year;
                mesHasta = month;
                diaHasta = dayOfMonth;
                btnFiltroHasta.setText(textoBoton);
            }
            cargarHistorial();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String isoInicioDia(Integer anio, Integer mes, Integer dia) {
        if (anio == null) {
            return null;
        }
        return String.format(Locale.US, "%04d-%02d-%02dT00:00:00Z", anio, mes + 1, dia);
    }

    private String isoFinDia(Integer anio, Integer mes, Integer dia) {
        if (anio == null) {
            return null;
        }
        return String.format(Locale.US, "%04d-%02d-%02dT23:59:59Z", anio, mes + 1, dia);
    }

    private void cargarHistorial() {
        String tipo = TIPOS_API[spinnerTipo.getSelectedItemPosition()];
        String desde = isoInicioDia(anioDesde, mesDesde, diaDesde);
        String hasta = isoFinDia(anioHasta, mesHasta, diaHasta);

        ApiClient.api().getHistorial(tipo, desde, hasta)
                .enqueue(new Callback<List<ApiClient.HistorialItemResponse>>() {
                    @Override
                    public void onResponse(
                            Call<List<ApiClient.HistorialItemResponse>> call,
                            Response<List<ApiClient.HistorialItemResponse>> response
                    ) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(
                                    HistorialOperacionesActivity.this,
                                    ApiClient.errorMessage(response, "No se pudo cargar el historial"),
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }
                        mostrarHistorial(response.body());
                    }

                    @Override
                    public void onFailure(Call<List<ApiClient.HistorialItemResponse>> call, Throwable error) {
                        Toast.makeText(
                                HistorialOperacionesActivity.this,
                                "No se pudo conectar con el servidor",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void mostrarHistorial(List<ApiClient.HistorialItemResponse> operaciones) {
        contenedor.removeAllViews();

        if (operaciones.isEmpty()) {
            tvSinOperaciones.setVisibility(View.VISIBLE);
            return;
        }
        tvSinOperaciones.setVisibility(View.GONE);

        for (ApiClient.HistorialItemResponse operacion : operaciones) {
            contenedor.addView(crearTarjeta(operacion));
        }
    }

    private MaterialCardView crearTarjeta(ApiClient.HistorialItemResponse operacion) {
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
        contenido.setPadding(dp(20), dp(16), dp(20), dp(16));
        tarjeta.addView(contenido);

        TextView tipo = new TextView(this);
        boolean esVenta = "VENTA".equals(operacion.getTipo());
        tipo.setText(esVenta ? "VENTA" : "COMPRA");
        tipo.setTextColor(esVenta ? getColor(R.color.ronda_primary) : getColor(R.color.ronda_success));
        tipo.setTextSize(13);
        tipo.setTypeface(null, android.graphics.Typeface.BOLD);
        contenido.addView(tipo);

        TextView titulo = new TextView(this);
        titulo.setText(operacion.getPublicacionTitulo());
        titulo.setTextColor(getColor(R.color.ronda_text_primary));
        titulo.setTextSize(18);
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tituloParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tituloParams.topMargin = dp(4);
        titulo.setLayoutParams(tituloParams);
        contenido.addView(titulo);

        TextView detalle = new TextView(this);
        detalle.setText(String.format(Locale.getDefault(), "%s • $ %.2f • %s",
                formatFecha(operacion.getFechaOperacion()), operacion.getMontoFinal(),
                operacion.getContraparteNombre()));
        detalle.setTextColor(getColor(R.color.ronda_text_secondary));
        detalle.setTextSize(14);
        LinearLayout.LayoutParams detalleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detalleParams.topMargin = dp(8);
        detalle.setLayoutParams(detalleParams);
        detalle.setOnClickListener(view -> {
            Intent intent = new Intent(this, PerfilUsuarioActivity.class);
            intent.putExtra("USUARIO_ID", operacion.getContraparteId());
            startActivity(intent);
        });
        contenido.addView(detalle);

        if (operacion.puedeCalificarAhora()) {
            MaterialButton btnCalificar = new MaterialButton(this);
            btnCalificar.setText("Calificar");
            btnCalificar.setAllCaps(false);
            btnCalificar.setTextColor(getColor(android.R.color.white));
            btnCalificar.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.ronda_primary)));
            btnCalificar.setCornerRadius(dp(14));
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
            btnParams.topMargin = dp(14);
            btnCalificar.setLayoutParams(btnParams);
            btnCalificar.setOnClickListener(view -> {
                Intent intent = new Intent(this, CalificarActivity.class);
                intent.putExtra("OPERACION_ID", operacion.getId());
                intent.putExtra("CONTRAPARTE_NOMBRE", operacion.getContraparteNombre());
                intent.putExtra("PUBLICACION_TITULO", operacion.getPublicacionTitulo());
                startActivity(intent);
            });
            contenido.addView(btnCalificar);
        } else if (operacion.isYaCalificada()) {
            TextView yaCalificada = new TextView(this);
            yaCalificada.setText("Ya calificaste esta operación");
            yaCalificada.setTextColor(getColor(R.color.ronda_success));
            yaCalificada.setTextSize(13);
            yaCalificada.setGravity(Gravity.START);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(10);
            yaCalificada.setLayoutParams(params);
            contenido.addView(yaCalificada);
        }

        return tarjeta;
    }

    private String formatFecha(String isoInstant) {
        if (isoInstant == null || isoInstant.length() < 10) {
            return "";
        }
        String[] partes = isoInstant.substring(0, 10).split("-");
        return partes[2] + "/" + partes[1] + "/" + partes[0];
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
