package com.example.ronda;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PublicarArticuloActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_publicar_articulo);

        EditText etTitulo = findViewById(R.id.etTituloPublicacion);
        EditText etDescripcion = findViewById(R.id.etDescripcionPublicacion);
        EditText etCategoria = findViewById(R.id.etCategoriaPublicacion);
        EditText etPrecio = findViewById(R.id.etPrecioPublicacion);
        EditText etDireccion = findViewById(R.id.etDireccionPublicacion);
        EditText etLatitud = findViewById(R.id.etLatitud);
        EditText etLongitud = findViewById(R.id.etLongitud);

        TextInputLayout tilTitulo = findViewById(R.id.tilTituloPublicacion);
        TextInputLayout tilDescripcion = findViewById(R.id.tilDescripcionPublicacion);
        TextInputLayout tilCategoria = findViewById(R.id.tilCategoriaPublicacion);
        TextInputLayout tilPrecio = findViewById(R.id.tilPrecioPublicacion);
        TextInputLayout tilDireccion = findViewById(R.id.tilDireccionPublicacion);
        TextInputLayout tilLatitud = findViewById(R.id.tilLatitud);
        TextInputLayout tilLongitud = findViewById(R.id.tilLongitud);

        Spinner spinnerEstado = findViewById(R.id.spinnerEstadoArticulo);
        Button btnCrearPublicacion = findViewById(R.id.btnCrearPublicacion);

        btnCrearPublicacion.setOnClickListener(view -> {
            String titulo = etTitulo.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();
            String categoria = etCategoria.getText().toString().trim();
            String precioTexto = etPrecio.getText().toString().trim();
            String direccion = etDireccion.getText().toString().trim();
            String latitudTexto = etLatitud.getText().toString().trim();
            String longitudTexto = etLongitud.getText().toString().trim();
            String estadoArticulo = spinnerEstado.getSelectedItem().toString();

            tilTitulo.setError(null);
            tilDescripcion.setError(null);
            tilCategoria.setError(null);
            tilPrecio.setError(null);
            tilDireccion.setError(null);
            tilLatitud.setError(null);
            tilLongitud.setError(null);

            if (titulo.isEmpty()) {
                tilTitulo.setError("Ingresá un título");
                return;
            }

            if (descripcion.isEmpty()) {
                tilDescripcion.setError("Ingresá una descripción");
                return;
            }

            if (categoria.isEmpty()) {
                tilCategoria.setError("Ingresá una categoría");
                return;
            }

            double precio;
            try {
                precio = Double.parseDouble(precioTexto.replace(",", "."));
                if (precio <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException error) {
                tilPrecio.setError("Ingresá un precio válido");
                return;
            }

            if (direccion.isEmpty()) {
                tilDireccion.setError("Ingresá una dirección");
                return;
            }

            double latitud;
            try {
                latitud = Double.parseDouble(latitudTexto.replace(",", "."));
                if (latitud < -90 || latitud > 90) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException error) {
                tilLatitud.setError("Ingresá una latitud válida");
                return;
            }

            double longitud;
            try {
                longitud = Double.parseDouble(longitudTexto.replace(",", "."));
                if (longitud < -180 || longitud > 180) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException error) {
                tilLongitud.setError("Ingresá una longitud válida");
                return;
            }

            btnCrearPublicacion.setEnabled(false);

            ApiClient.CreatePublicacionRequest request =
                    new ApiClient.CreatePublicacionRequest(
                            titulo,
                            descripcion,
                            categoria,
                            precio,
                            estadoArticulo,
                            direccion,
                            latitud,
                            longitud
                    );

            ApiClient.api().createPublicacion(request)
                    .enqueue(new Callback<ApiClient.PublicacionResponse>() {
                        @Override
                        public void onResponse(
                                Call<ApiClient.PublicacionResponse> call,
                                Response<ApiClient.PublicacionResponse> response
                        ) {
                            btnCrearPublicacion.setEnabled(true);

                            if (response.code() == 201) {
                                Toast.makeText(
                                        PublicarArticuloActivity.this,
                                        "Publicación creada correctamente",
                                        Toast.LENGTH_LONG
                                ).show();
                                finish();
                                return;
                            }

                            Toast.makeText(
                                    PublicarArticuloActivity.this,
                                    ApiClient.errorMessage(
                                            response,
                                            "No se pudo crear la publicación"
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        @Override
                        public void onFailure(
                                Call<ApiClient.PublicacionResponse> call,
                                Throwable error
                        ) {
                            btnCrearPublicacion.setEnabled(true);

                            Toast.makeText(
                                    PublicarArticuloActivity.this,
                                    "No se pudo conectar con el servidor",
                                    Toast.LENGTH_LONG
                            ).show();
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