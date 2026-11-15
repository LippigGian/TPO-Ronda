package com.example.ronda.recepcion;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ronda.ApiClient;
import com.example.ronda.CalificarActivity;
import com.example.ronda.ConnectivityObserver;
import com.example.ronda.Formato;
import com.example.ronda.R;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Cierre de la operación con QR: el comprador escanea el QR físico de entrega para confirmar que
 * recibió el artículo. La app no genera el QR: lo lee con la cámara (o desde una imagen de la galería)
 * y envía su texto al backend, que valida y cierra la operación.
 *
 * Formato esperado del QR: "RONDA-ENTREGA-{idPublicacion}".
 */
public class ConfirmarRecepcionActivity extends AppCompatActivity {
    public static final String EXTRA_PUBLICACION_ID = "PUBLICACION_ID";
    public static final String EXTRA_TITULO = "PUBLICACION_TITULO";

    /** Mismo formato que valida el backend (CodigoEntrega): se usa solo para detectar QR de otra publicación. */
    private static final Pattern CODIGO_ENTREGA =
            Pattern.compile("^RONDA-ENTREGA-(\\d{1,18})$", Pattern.CASE_INSENSITIVE);

    private long publicacionId;
    private String titulo;

    private PreviewView previewView;
    private View grupoPie;
    private TextView tvAvisoCamara;
    private View progreso;
    private View panelResultado;
    private TextView tvIcono;
    private TextView tvTituloResultado;
    private TextView tvMensajeResultado;
    private MaterialButton btnPrimario;
    private MaterialButton btnSecundario;

    private BarcodeScanner scanner;
    private LifecycleCameraController camara;
    /** Evita procesar el mismo QR varias veces: la cámara entrega muchos cuadros por segundo. */
    private boolean procesando;

    private final ActivityResultLauncher<String> permisoCamara =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    iniciarCamara();
                } else {
                    tvAvisoCamara.setVisibility(View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<String> elegirImagen =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    leerQrDeImagen(uri);
                }
            });

    public static Intent crearIntent(Context context, long publicacionId, String titulo) {
        return new Intent(context, ConfirmarRecepcionActivity.class)
                .putExtra(EXTRA_PUBLICACION_ID, publicacionId)
                .putExtra(EXTRA_TITULO, titulo);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirmar_recepcion);

        publicacionId = getIntent().getLongExtra(EXTRA_PUBLICACION_ID, -1);
        titulo = getIntent().getStringExtra(EXTRA_TITULO);
        if (publicacionId < 0) {
            finish();
            return;
        }

        previewView = findViewById(R.id.previewQr);
        grupoPie = findViewById(R.id.grupoPieQr);
        tvAvisoCamara = findViewById(R.id.tvAvisoCamara);
        progreso = findViewById(R.id.progresoQr);
        panelResultado = findViewById(R.id.panelResultadoQr);
        tvIcono = findViewById(R.id.tvIconoResultadoQr);
        tvTituloResultado = findViewById(R.id.tvTituloResultadoQr);
        tvMensajeResultado = findViewById(R.id.tvMensajeResultadoQr);
        btnPrimario = findViewById(R.id.btnPrimarioResultadoQr);
        btnSecundario = findViewById(R.id.btnSecundarioResultadoQr);

        ((TextView) findViewById(R.id.tvArticuloQr))
                .setText("Confirmá que recibiste «" + titulo + "»");
        findViewById(R.id.btnElegirImagenQr).setOnClickListener(v -> elegirImagen.launch("image/*"));
        findViewById(R.id.btnCancelarQr).setOnClickListener(v -> finish());

        // Solo QR: más rápido y evita leer otros códigos de barras por error.
        scanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            permisoCamara.launch(Manifest.permission.CAMERA);
        }

        aplicarInsets();
    }

    @Override
    protected void onDestroy() {
        if (scanner != null) {
            scanner.close();
        }
        super.onDestroy();
    }

    // ---------- Lectura del QR ----------

    private void iniciarCamara() {
        tvAvisoCamara.setVisibility(View.GONE);
        camara = new LifecycleCameraController(this);
        camara.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(this),
                new MlKitAnalyzer(Collections.singletonList(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                        ContextCompat.getMainExecutor(this), resultado -> {
                            List<Barcode> codigos = resultado.getValue(scanner);
                            if (codigos != null && !codigos.isEmpty()) {
                                alLeerCodigo(codigos.get(0).getRawValue());
                            }
                        }));
        // Algunos dispositivos (y emuladores) solo tienen cámara frontal.
        camara.getInitializationFuture().addListener(() -> {
            if (!camara.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                    && camara.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                camara.setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA);
            }
        }, ContextCompat.getMainExecutor(this));
        camara.bindToLifecycle(this);
        previewView.setController(camara);
    }

    private void leerQrDeImagen(Uri uri) {
        if (procesando) {
            return;
        }
        InputImage imagen;
        try {
            imagen = InputImage.fromFilePath(this, uri);
        } catch (IOException error) {
            mostrarError("No se pudo abrir la imagen", "Probá con otra imagen del QR.");
            return;
        }
        progreso.setVisibility(View.VISIBLE);
        scanner.process(imagen)
                .addOnSuccessListener(codigos -> {
                    progreso.setVisibility(View.GONE);
                    if (codigos.isEmpty() || codigos.get(0).getRawValue() == null) {
                        mostrarError("No se encontró un QR",
                                "La imagen elegida no contiene un código QR legible. Probá con otra imagen.");
                    } else {
                        alLeerCodigo(codigos.get(0).getRawValue());
                    }
                })
                .addOnFailureListener(error -> {
                    progreso.setVisibility(View.GONE);
                    mostrarError("No se pudo leer la imagen", "Probá con otra imagen del QR.");
                });
    }

    private void alLeerCodigo(String texto) {
        if (procesando || texto == null) {
            return;
        }
        procesando = true;

        // Un QR de Ronda, pero de otra publicación: se avisa sin cerrar otra operación por error.
        Matcher matcher = CODIGO_ENTREGA.matcher(texto.strip());
        if (matcher.matches() && Long.parseLong(matcher.group(1)) != publicacionId) {
            mostrarError("Código inválido",
                    "Este QR corresponde a otra publicación. Escaneá el QR de entrega de «" + titulo + "».");
            return;
        }

        if (!ConnectivityObserver.isOnline(this)) {
            mostrarError("Sin conexión", "Necesitás conexión a internet para confirmar la recepción.");
            return;
        }

        confirmarEnServidor(texto);
    }

    // ---------- Backend ----------

    private void confirmarEnServidor(String codigo) {
        progreso.setVisibility(View.VISIBLE);
        ApiClient.api()
                .confirmarRecepcion(new ApiClient.ConfirmarRecepcionRequest(codigo))
                .enqueue(new Callback<ApiClient.HistorialItemResponse>() {
                    @Override
                    public void onResponse(Call<ApiClient.HistorialItemResponse> call,
                                           Response<ApiClient.HistorialItemResponse> response) {
                        progreso.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            mostrarExito(response.body());
                            return;
                        }
                        String titulo = response.code() == 409 ? "Recepción ya confirmada" : "Código inválido";
                        mostrarError(titulo, sinPrefijoCodigoInvalido(ApiClient.errorMessage(response,
                                "El código escaneado no es válido para esta operación.")));
                    }

                    @Override
                    public void onFailure(Call<ApiClient.HistorialItemResponse> call, Throwable error) {
                        progreso.setVisibility(View.GONE);
                        mostrarError("No se pudo confirmar",
                                "No se pudo conectar con el servidor. Revisá tu conexión y probá de nuevo.");
                    }
                });
    }

    /** "Código inválido: no es un QR..." → "No es un QR...", porque el título ya dice "Código inválido". */
    private static String sinPrefijoCodigoInvalido(String mensaje) {
        String prefijo = "Código inválido: ";
        if (!mensaje.startsWith(prefijo) || mensaje.length() == prefijo.length()) {
            return mensaje;
        }
        String resto = mensaje.substring(prefijo.length());
        return Character.toUpperCase(resto.charAt(0)) + resto.substring(1) + ".";
    }

    // ---------- Resultado ----------

    private void mostrarExito(ApiClient.HistorialItemResponse operacion) {
        mostrarPanel("✓", R.color.ronda_success, "Operación confirmada",
                "Recibiste «" + operacion.getPublicacionTitulo() + "». Se registró la compra por "
                        + Formato.precio(operacion.getMontoFinal()) + " al vendedor "
                        + operacion.getContraparteNombre()
                        + ". La publicación quedó como vendida y la operación figura en el historial de ambos.");

        btnPrimario.setText("Calificar al vendedor");
        btnPrimario.setOnClickListener(v -> {
            startActivity(new Intent(this, CalificarActivity.class)
                    .putExtra("OPERACION_ID", operacion.getId())
                    .putExtra("CONTRAPARTE_NOMBRE", operacion.getContraparteNombre())
                    .putExtra("PUBLICACION_TITULO", operacion.getPublicacionTitulo()));
            finish();
        });
        btnSecundario.setText("Volver");
        btnSecundario.setOnClickListener(v -> finish());
    }

    private void mostrarError(String titulo, String mensaje) {
        mostrarPanel("✕", R.color.oferta_rechazada, titulo, mensaje);

        btnPrimario.setText("Escanear de nuevo");
        btnPrimario.setOnClickListener(v -> {
            panelResultado.setVisibility(View.GONE);
            grupoPie.setVisibility(View.VISIBLE);
            procesando = false;
        });
        btnSecundario.setText("Cancelar");
        btnSecundario.setOnClickListener(v -> finish());
    }

    private void mostrarPanel(String icono, @ColorRes int color, String titulo, String mensaje) {
        tvIcono.setText(icono);
        tvIcono.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, color)));
        tvTituloResultado.setText(titulo);
        tvMensajeResultado.setText(mensaje);
        grupoPie.setVisibility(View.GONE);
        panelResultado.setVisibility(View.VISIBLE);
    }

    private void aplicarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
