package com.example.ronda;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputLayout;
import java.util.concurrent.Executor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final int LOCAL_NETWORK_PERMISSION_REQUEST_CODE = 100;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        sessionManager = new SessionManager(this);
        requestLocalNetworkPermissionIfNeeded();

        // Un token guardado indica que existe una sesión previa. Para reutilizarla
        // se exige una autenticación biométrica antes de abrir el inicio.
        if (sessionManager.getToken() != null) {
            solicitarBiometria();
        }

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        TextInputLayout tilEmail = findViewById(R.id.tilEmail);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnOtp = findViewById(R.id.btnOtp);
        Button btnRecuperarAcceso = findViewById(R.id.btnRecuperarAcceso);
        Button btnCrearCuenta = findViewById(R.id.btnCrearCuenta);

        btnOtp.setOnClickListener(view -> startActivity(
                new Intent(LoginActivity.this, SolicitarOtpActivity.class)
        ));

        btnLogin.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (email.isEmpty()) {
                tilEmail.setError("Ingresá tu correo electrónico");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Ingresá un correo electrónico válido");
                return;
            }
            tilEmail.setError(null);
            if (password.isEmpty()) {
                etPassword.setError("Ingresá tu contraseña");
                return;
            }

            btnLogin.setEnabled(false);
            ApiClient.api().login(new ApiClient.LoginRequest(email, password))
                    .enqueue(new Callback<ApiClient.LoginResponse>() {
                        @Override
                        public void onResponse(Call<ApiClient.LoginResponse> call,
                                               Response<ApiClient.LoginResponse> response) {
                            btnLogin.setEnabled(true);
                            ApiClient.LoginResponse body = response.body();

                            if (response.isSuccessful() && body != null && body.getToken() != null) {
                                saveSessionAndOpenHome(body.getToken());
                                return;
                            }

                            String message = response.code() == 401 || response.code() == 403
                                    ? "Email o contraseña incorrectos"
                                    : ApiClient.errorMessage(response, "No se pudo iniciar sesión");
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(Call<ApiClient.LoginResponse> call, Throwable error) {
                            btnLogin.setEnabled(true);
                            Log.e("LoginActivity", "Error al iniciar sesión contra el backend", error);
                            Toast.makeText(LoginActivity.this,
                                    "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnRecuperarAcceso.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SolicitarOtpActivity.class);
            intent.putExtra("OTP_PURPOSE", "RECUPERO_CONTRASENA");
            startActivity(intent);
        });
        btnCrearCuenta.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegistroActivity.class))
        );

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void solicitarBiometria() {
        int disponibilidad = BiometricManager.from(this)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if (disponibilidad != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this,
                    "Configurá una huella o reconocimiento facial para desbloquear la sesión",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        abrirInicio();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear Ronda")
                .setSubtitle("Confirmá tu identidad para continuar")
                .setNegativeButtonText("Cancelar")
                .build();
        prompt.authenticate(promptInfo);
    }

    private void requestLocalNetworkPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 37
                && checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_LOCAL_NETWORK},
                    LOCAL_NETWORK_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void saveSessionAndOpenHome(String token) {
        sessionManager.saveToken(token);
        abrirInicio();
    }

    private void abrirInicio() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
