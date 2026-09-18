package com.example.ronda;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
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
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class PublicarArticuloActivity extends AppCompatActivity {
    private final List<Uri> fotosSeleccionadas = new ArrayList<>();
    private ActivityResultLauncher<String> selectorFotos;
    private TextView tvFotosSeleccionadas;
    private EditText etTitulo;
    private EditText etDescripcion;
    private EditText etCategoria;
    private EditText etPrecio;
    private EditText etDireccion;
    private EditText etLatitud;
    private EditText etLongitud;
    private Spinner spinnerEstado;
    private BorradorPublicacionManager borradorManager;
    private boolean publicacionCreada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_publicar_articulo);

        etTitulo = findViewById(R.id.etTituloPublicacion);
        etDescripcion = findViewById(R.id.etDescripcionPublicacion);
        etCategoria = findViewById(R.id.etCategoriaPublicacion);
        etPrecio = findViewById(R.id.etPrecioPublicacion);
        etDireccion = findViewById(R.id.etDireccionPublicacion);
        etLatitud = findViewById(R.id.etLatitud);
        etLongitud = findViewById(R.id.etLongitud);

        TextInputLayout tilTitulo = findViewById(R.id.tilTituloPublicacion);
        TextInputLayout tilDescripcion = findViewById(R.id.tilDescripcionPublicacion);
        TextInputLayout tilCategoria = findViewById(R.id.tilCategoriaPublicacion);
        TextInputLayout tilPrecio = findViewById(R.id.tilPrecioPublicacion);
        TextInputLayout tilDireccion = findViewById(R.id.tilDireccionPublicacion);
        TextInputLayout tilLatitud = findViewById(R.id.tilLatitud);
        TextInputLayout tilLongitud = findViewById(R.id.tilLongitud);

        spinnerEstado = findViewById(R.id.spinnerEstadoArticulo);
        Button btnSeleccionarFotos = findViewById(R.id.btnSeleccionarFotos);
        Button btnCrearPublicacion = findViewById(R.id.btnCrearPublicacion);
        tvFotosSeleccionadas = findViewById(R.id.tvFotosSeleccionadas);

        borradorManager = BorradorPublicacionManager.getInstance(this);
        restaurarBorrador();

        selectorFotos = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    fotosSeleccionadas.clear();

                    for (Uri uri : uris) {
                        if (fotosSeleccionadas.size() == 5) {
                            break;
                        }
                        fotosSeleccionadas.add(uri);
                    }

                    if (uris.size() > 5) {
                        Toast.makeText(
                                this,
                                "Solo se seleccionaron las primeras 5 fotos",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    if (fotosSeleccionadas.isEmpty()) {
                        tvFotosSeleccionadas.setText("Podés seleccionar hasta 5 fotos.");
                    } else {
                        tvFotosSeleccionadas.setText(
                                fotosSeleccionadas.size() + " foto(s) seleccionada(s)"
                        );
                    }
                }
        );

        btnSeleccionarFotos.setOnClickListener(view -> selectorFotos.launch("image/*"));

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

            if (fotosSeleccionadas.isEmpty()) {
                Toast.makeText(
                        this,
                        "Seleccioná al menos una foto para publicar",
                        Toast.LENGTH_LONG
                ).show();
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
                            if (response.code() == 201 && response.body() != null) {
                                if (fotosSeleccionadas.isEmpty()) {
                                    btnCrearPublicacion.setEnabled(true);

                                    Toast.makeText(
                                            PublicarArticuloActivity.this,
                                            "Publicación creada correctamente",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    publicacionCreada = true;
                                    borradorManager.borrar();
                                    finish();
                                    return;
                                }

                                subirFotos(response.body().getId(), btnCrearPublicacion);
                                return;
                            }

                            btnCrearPublicacion.setEnabled(true);

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
    private void subirFotos(
            long publicacionId,
            Button btnCrearPublicacion
    ) {
        List<MultipartBody.Part> partes;

        try {
            partes = crearPartesFotos();
        } catch (IOException error) {
            btnCrearPublicacion.setEnabled(true);

            Toast.makeText(
                    this,
                    "La publicación se creó, pero no se pudieron preparar las fotos",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        ApiClient.api()
                .uploadFotos(publicacionId, partes)
                .enqueue(new Callback<ApiClient.PublicacionResponse>() {
                    @Override
                    public void onResponse(
                            Call<ApiClient.PublicacionResponse> call,
                            Response<ApiClient.PublicacionResponse> response
                    ) {
                        btnCrearPublicacion.setEnabled(true);

                        if (response.isSuccessful()) {
                            Toast.makeText(
                                    PublicarArticuloActivity.this,
                                    "Publicación creada con sus fotos",
                                    Toast.LENGTH_LONG
                            ).show();

                            publicacionCreada = true;
                            borradorManager.borrar();
                            finish();
                            return;
                        }

                        Toast.makeText(
                                PublicarArticuloActivity.this,
                                ApiClient.errorMessage(
                                        response,
                                        "La publicación se creó, pero no se pudieron subir las fotos"
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
                                "La publicación se creó, pero no se pudieron subir las fotos",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private List<MultipartBody.Part> crearPartesFotos() throws IOException {
        List<MultipartBody.Part> partes = new ArrayList<>();

        for (int i = 0; i < fotosSeleccionadas.size(); i++) {
            Uri uri = fotosSeleccionadas.get(i);
            String contentType = getContentResolver().getType(uri);

            if (contentType == null) {
                contentType = "image/jpeg";
            }

            InputStream input = getContentResolver().openInputStream(uri);

            if (input == null) {
                throw new IOException("No se pudo leer la imagen");
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            input.close();

            RequestBody body = RequestBody.create(
                    MediaType.parse(contentType),
                    output.toByteArray()
            );

            MultipartBody.Part parte = MultipartBody.Part.createFormData(
                    "fotos",
                    "foto_" + (i + 1) + ".jpg",
                    body
            );

            partes.add(parte);
        }

        return partes;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (borradorManager == null || publicacionCreada) {
            return;
        }

        borradorManager.guardar(new BorradorPublicacionManager.Borrador(
                etTitulo.getText().toString(),
                etDescripcion.getText().toString(),
                etCategoria.getText().toString(),
                etPrecio.getText().toString(),
                etDireccion.getText().toString(),
                etLatitud.getText().toString(),
                etLongitud.getText().toString(),
                spinnerEstado.getSelectedItem().toString()
        ));
    }

    private void restaurarBorrador() {
        borradorManager.leer(borrador -> runOnUiThread(() -> {
            if (borrador.estaVacio()) {
                return;
            }

            etTitulo.setText(borrador.titulo);
            etDescripcion.setText(borrador.descripcion);
            etCategoria.setText(borrador.categoria);
            etPrecio.setText(borrador.precio);
            etDireccion.setText(borrador.direccion);
            etLatitud.setText(borrador.latitud);
            etLongitud.setText(borrador.longitud);
            seleccionarEstadoGuardado(borrador.estadoArticulo);

            Toast.makeText(
                    this,
                    "Recuperamos el borrador de tu publicación",
                    Toast.LENGTH_LONG
            ).show();
        }));
    }

    private void seleccionarEstadoGuardado(String estadoGuardado) {
        for (int i = 0; i < spinnerEstado.getCount(); i++) {
            if (spinnerEstado.getItemAtPosition(i).toString().equals(estadoGuardado)) {
                spinnerEstado.setSelection(i);
                return;
            }
        }
    }
}
