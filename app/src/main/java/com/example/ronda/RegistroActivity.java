package com.example.ronda;

import android.content.Intent;
import android.os.Bundle;
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

public class RegistroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        EditText etEmail = findViewById(R.id.etRegisterEmail);
        EditText etPassword = findViewById(R.id.etRegisterPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);

        TextInputLayout tilEmail = findViewById(R.id.tilRegisterEmail);
        TextInputLayout tilPassword = findViewById(R.id.tilRegisterPassword);
        TextInputLayout tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        Button btnRegistrarse = findViewById(R.id.btnRegistrarse);

        btnRegistrarse.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            tilEmail.setError(null);
            tilPassword.setError(null);
            tilConfirmPassword.setError(null);

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Ingresá un correo electrónico válido");
                return;
            }

            if (password.length() < 8) {
                tilPassword.setError("La contraseña debe tener al menos 8 caracteres");
                return;
            }

            if (!password.equals(confirmPassword)) {
                tilConfirmPassword.setError("Las contraseñas no coinciden");
                return;
            }

            btnRegistrarse.setEnabled(false);

            ApiClient.api()
                    .register(new ApiClient.RegisterRequest(email, password))
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(
                                Call<Void> call,
                                Response<Void> response
                        ) {
                            btnRegistrarse.setEnabled(true);

                            if (response.code() == 201) {
                                Intent intent = new Intent(
                                        RegistroActivity.this,
                                        ValidarOtpActivity.class
                                );
                                intent.putExtra("EMAIL", email);
                                intent.putExtra("OTP_PURPOSE", "REGISTRO");
                                startActivity(intent);
                                finish();
                                return;
                            }

                            String message = ApiClient.errorMessage(
                                    response,
                                    "No se pudo crear la cuenta"
                            );

                            if (response.code() == 409) {
                                tilEmail.setError("Ya existe una cuenta con ese email");
                            } else {
                                Toast.makeText(
                                        RegistroActivity.this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable error) {
                            btnRegistrarse.setEnabled(true);
                            Toast.makeText(
                                    RegistroActivity.this,
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
