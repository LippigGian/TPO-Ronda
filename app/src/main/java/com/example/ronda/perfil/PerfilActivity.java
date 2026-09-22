package com.example.ronda.perfil;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.ronda.ApiClient;
import com.example.ronda.ConnectivityObserver;
import com.example.ronda.R;
import com.example.ronda.SessionManager;
import com.example.ronda.util.ApiCallback;
import com.example.ronda.util.MultipartHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

import okhttp3.MultipartBody;

/** Punto 2: ver y editar los datos personales propios, y ver la reputacion. */
public class PerfilActivity extends AppCompatActivity {

    /** Mismo criterio que la validacion del backend (PerfilDtos.ActualizarPerfilRequest). */
    private static final String REGEX_TELEFONO = "^[0-9+()\\s-]{6,30}$";

    private PerfilApi api;
    private SessionManager sessionManager;
    private ActivityResultLauncher<String> selectorFoto;
    private long miUsuarioId = -1;

    private ShapeableImageView ivFoto;
    private TextView tvEstrellas;
    private TextView tvOperaciones;
    private TextView tvMiembroDesde;
    private TextInputLayout tilEmail;
    private TextInputLayout tilNombre;
    private TextInputLayout tilTelefono;
    private TextInputEditText etEmail;
    private TextInputEditText etNombre;
    private TextInputEditText etTelefono;
    private TextInputEditText etZona;
    private MaterialButton btnGuardar;
    private MaterialButton btnCambiarFoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil);
        aplicarInsets();

        api = ApiClient.crearServicio(PerfilApi.class);
        sessionManager = new SessionManager(this);
        vincularVistas();

        // Abre la galeria; cuando el usuario elige una imagen, se sube al backend.
        selectorFoto = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        subirFoto(uri);
                    }
                }
        );

        btnCambiarFoto.setOnClickListener(v -> {
            if (hayConexion()) {
                selectorFoto.launch("image/*");
            }
        });
        btnGuardar.setOnClickListener(v -> guardar());
        findViewById(R.id.btnVerPerfilPublico).setOnClickListener(v -> {
            if (miUsuarioId > 0) {
                PerfilPublicoActivity.abrir(this, miUsuarioId);
            }
        });

        // Se carga en onCreate (no en onResume) para no pisar lo que el usuario
        // esta escribiendo cuando vuelve de la galeria.
        cargarPerfil();
    }

    private void vincularVistas() {
        ivFoto = findViewById(R.id.ivFotoPerfil);
        tvEstrellas = findViewById(R.id.tvEstrellas);
        tvOperaciones = findViewById(R.id.tvOperaciones);
        tvMiembroDesde = findViewById(R.id.tvMiembroDesde);
        tilEmail = findViewById(R.id.tilEmail);
        tilNombre = findViewById(R.id.tilNombre);
        tilTelefono = findViewById(R.id.tilTelefono);
        etEmail = findViewById(R.id.etEmail);
        etNombre = findViewById(R.id.etNombre);
        etTelefono = findViewById(R.id.etTelefono);
        etZona = findViewById(R.id.etZona);
        btnGuardar = findViewById(R.id.btnGuardarPerfil);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
    }

    // ---------- Llamadas al backend ----------

    private void cargarPerfil() {
        api.obtenerMiPerfil().enqueue(new ApiCallback<PerfilModels.MiPerfil>(this, "No se pudo cargar tu perfil") {
            @Override
            protected void onExito(PerfilModels.MiPerfil perfil) {
                mostrarDatos(perfil);
                mostrarEncabezado(perfil);
            }
        });
    }

    private void guardar() {
        if (!hayConexion() || !formularioValido()) {
            return;
        }

        PerfilModels.ActualizarPerfilRequest request = new PerfilModels.ActualizarPerfilRequest(
                texto(etEmail), texto(etNombre), texto(etTelefono), texto(etZona));

        btnGuardar.setEnabled(false);
        api.actualizarMiPerfil(request).enqueue(new ApiCallback<PerfilModels.ActualizarPerfilResponse>(this, "No se pudo guardar el perfil") {
            @Override
            protected void onExito(PerfilModels.ActualizarPerfilResponse respuesta) {
                // Si el email cambio, el backend reemite el token (el anterior queda invalido).
                if (respuesta.getToken() != null) {
                    sessionManager.saveToken(respuesta.getToken());
                }
                mostrarDatos(respuesta.getPerfil());
                mostrarEncabezado(respuesta.getPerfil());
                Toast.makeText(PerfilActivity.this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
            }

            @Override
            protected void onFinalizar() {
                btnGuardar.setEnabled(true);
            }
        });
    }

    private void subirFoto(Uri uri) {
        MultipartBody.Part parte;
        try {
            parte = MultipartHelper.imagenDesdeUri(this, uri, "foto", "perfil.jpg");
        } catch (IOException e) {
            Toast.makeText(this, "No se pudo leer la imagen", Toast.LENGTH_LONG).show();
            return;
        }

        btnCambiarFoto.setEnabled(false);
        api.actualizarFoto(parte).enqueue(new ApiCallback<PerfilModels.MiPerfil>(this, "No se pudo actualizar la foto") {
            @Override
            protected void onExito(PerfilModels.MiPerfil perfil) {
                // Solo se refresca el encabezado para no pisar datos del formulario sin guardar.
                mostrarEncabezado(perfil);
                Toast.makeText(PerfilActivity.this, "Foto actualizada", Toast.LENGTH_SHORT).show();
            }

            @Override
            protected void onFinalizar() {
                btnCambiarFoto.setEnabled(true);
            }
        });
    }

    // ---------- Mostrar datos en pantalla ----------

    private void mostrarDatos(PerfilModels.MiPerfil perfil) {
        miUsuarioId = perfil.getId();
        etEmail.setText(perfil.getEmail());
        etNombre.setText(perfil.getNombre());
        etTelefono.setText(perfil.getTelefono());
        etZona.setText(perfil.getZona());
    }

    private void mostrarEncabezado(PerfilModels.MiPerfil perfil) {
        tvEstrellas.setText(PerfilFormatter.estrellas(perfil.getReputacion()));
        tvOperaciones.setText(PerfilFormatter.operaciones(perfil.getReputacion()));
        tvMiembroDesde.setText(PerfilFormatter.miembroDesde(perfil.getMiembroDesde()));

        String url = perfil.getFoto() == null ? null : ApiClient.imageUrl(perfil.getFoto());
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_perfil_placeholder)
                .fallback(R.drawable.ic_perfil_placeholder)
                .error(R.drawable.ic_perfil_placeholder)
                .centerCrop()
                .into(ivFoto);
    }

    // ---------- Validaciones y helpers ----------

    private boolean formularioValido() {
        boolean valido = true;

        tilEmail.setError(null);
        tilNombre.setError(null);
        tilTelefono.setError(null);

        String email = texto(etEmail);
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Ingresá tu email");
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Ingresá un email válido");
            valido = false;
        }

        if (TextUtils.isEmpty(texto(etNombre))) {
            tilNombre.setError("Ingresá tu nombre");
            valido = false;
        }

        String telefono = texto(etTelefono);
        if (!telefono.isEmpty() && !telefono.matches(REGEX_TELEFONO)) {
            tilTelefono.setError("Usá solo números, espacios, +, - o paréntesis (mínimo 6)");
            valido = false;
        }

        return valido;
    }

    /** Las acciones que modifican datos requieren conexion (punto 6 del TPO). */
    private boolean hayConexion() {
        if (ConnectivityObserver.isOnline(this)) {
            return true;
        }
        Toast.makeText(this, "Necesitás conexión a internet para continuar", Toast.LENGTH_LONG).show();
        return false;
    }

    private static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    private void aplicarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
