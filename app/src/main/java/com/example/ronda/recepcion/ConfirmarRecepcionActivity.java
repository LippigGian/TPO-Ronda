package com.example.ronda.recepcion;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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
import com.example.ronda.LoginActivity;
import com.example.ronda.R;
import com.example.ronda.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Cierre de la operación con QR: el comprador escanea el QR físico de entrega para confirmar que
 * recibió el artículo. La app no genera el QR: lo lee con la cámara (solo cámara, no desde imágenes
 * guardadas).
 *
 * 1. Pide permiso de cámara.
 * 2. Inicia CameraX con un analizador de ML Kit (setImageAnalysisAnalyzer + MlKitAnalyzer).
 * 3. ML Kit detecta el QR y devuelve el string crudo, que es un JSON serializado
 *    (ver {@link QrEntrega}): {"tipo": "RONDA-ENTREGA", "publicacionId": 12, "titulo": "..."}.
 * 4. La app lo parsea, valida que sea de esta publicación y envía el publicacionId al backend,
 *    que cierra la operación.
 */
public class ConfirmarRecepcionActivity extends AppCompatActivity {
    public static final String EXTRA_PUBLICACION_ID = "PUBLICACION_ID";
    public static final String EXTRA_TITULO = "PUBLICACION_TITULO";

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

    private void alLeerCodigo(String texto) {
        if (procesando || texto == null) {
            return;
        }
        procesando = true;

        // El QR trae un JSON serializado: se parsea antes de hablar con el servidor.
        QrEntrega qr;
        try {
            qr = QrEntrega.parsear(texto);
        } catch (QrEntrega.QrInvalidoException error) {
            mostrarError(ErrorRecepcion.qrInvalido());
            return;
        }

        // Un QR de Ronda, pero de otra publicación: se avisa sin cerrar otra operación por error.
        // No se muestra a qué artículo pertenece: el comprador solo necesita saber que no es el correcto.
        if (qr.getPublicacionId() != publicacionId) {
            mostrarError(ErrorRecepcion.otraPublicacion(titulo));
            return;
        }

        if (!ConnectivityObserver.isOnline(this)) {
            mostrarError(ErrorRecepcion.sinConexion());
            return;
        }

        confirmarEnServidor(qr.getPublicacionId());
    }

    // ---------- Backend ----------

    private void confirmarEnServidor(long idPublicacion) {
        progreso.setVisibility(View.VISIBLE);
        ApiClient.api()
                .confirmarRecepcion(new ApiClient.ConfirmarRecepcionRequest(idPublicacion))
                .enqueue(new Callback<ApiClient.HistorialItemResponse>() {
                    @Override
                    public void onResponse(Call<ApiClient.HistorialItemResponse> call,
                                           Response<ApiClient.HistorialItemResponse> response) {
                        progreso.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            mostrarExito(response.body());
                            return;
                        }
                        mostrarError(ErrorRecepcion.desdeRespuesta(response.code(), cuerpoVacio(response)));
                    }

                    @Override
                    public void onFailure(Call<ApiClient.HistorialItemResponse> call, Throwable error) {
                        progreso.setVisibility(View.GONE);
                        mostrarError(ErrorRecepcion.servidorInaccesible());
                    }
                });
    }

    /** Sesión vencida: el backend responde 403 sin cuerpo; los 403 de negocio traen un "detail". */
    private static boolean cuerpoVacio(Response<?> response) {
        if (response.errorBody() == null) {
            return true;
        }
        try {
            return response.errorBody().string().trim().isEmpty();
        } catch (IOException error) {
            return true;
        }
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
        btnSecundario.setVisibility(View.VISIBLE);
    }

    private void mostrarError(ErrorRecepcion error) {
        mostrarPanel("✕", R.color.oferta_rechazada, error.getTitulo(), error.getMensaje());
        btnSecundario.setText("Cancelar");
        btnSecundario.setOnClickListener(v -> finish());
        btnSecundario.setVisibility(View.VISIBLE);

        switch (error.getAccion()) {
            case INICIAR_SESION:
                btnPrimario.setText("Iniciar sesión");
                btnPrimario.setOnClickListener(v -> {
                    new SessionManager(this).clearToken();
                    startActivity(new Intent(this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                });
                break;
            case VOLVER:
                // Reintentar no cambia nada: solo se ofrece volver.
                btnPrimario.setText("Volver");
                btnPrimario.setOnClickListener(v -> finish());
                btnSecundario.setVisibility(View.GONE);
                break;
            default:
                btnPrimario.setText("Escanear de nuevo");
                btnPrimario.setOnClickListener(v -> {
                    panelResultado.setVisibility(View.GONE);
                    grupoPie.setVisibility(View.VISIBLE);
                    procesando = false;
                });
                break;
        }
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
