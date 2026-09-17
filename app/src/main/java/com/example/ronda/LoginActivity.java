package com.example.ronda;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
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

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        TextInputLayout tilEmail = findViewById(R.id.tilEmail);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnOtp = findViewById(R.id.btnOtp);
        Button btnRecuperarAcceso = findViewById(R.id.btnRecuperarAcceso);
        Button btnCrearCuenta = findViewById(R.id.btnCrearCuenta);


        /** Listener para boton OTP **/
        btnOtp.setOnClickListener(view -> startActivity(
                new Intent(LoginActivity.this, SolicitarOtpActivity.class)
        ));
        /** Listener para boton LOGIN **/
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
            /** Aplicamos retrofit y enqueue para iniciar el pedido en segundo plano**/
            ApiClient.api().login(new ApiClient.LoginRequest(email, password))
                    .enqueue(new Callback<ApiClient.LoginResponse>() {
                        @Override
                        /** Respuesta satisfactoria esperada**/
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
                        /** Respuesta fallida esperada **/
                        public void onFailure(Call<ApiClient.LoginResponse> call, Throwable error) {
                            btnLogin.setEnabled(true);
                            Log.e("LoginActivity", "Error al iniciar sesión contra el backend", error);
                            Toast.makeText(LoginActivity.this,
                                    "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show();
                        }
                    });
        });
        /** Listener para boton recuperar OTP **/
        btnRecuperarAcceso.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SolicitarOtpActivity.class);
            intent.putExtra("OTP_PURPOSE", "RECUPERO_CONTRASENA");
            startActivity(intent);
        });
        /**Listener para crear cuenta **/
        btnCrearCuenta.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegistroActivity.class))
        );

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /** GUARDAMOS EL TOKEN **/
    private void saveSessionAndOpenHome(String token) {
        new SessionManager(this).saveToken(token);
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
