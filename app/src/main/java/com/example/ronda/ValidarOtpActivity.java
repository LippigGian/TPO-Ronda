package com.example.ronda;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;
import android.widget.Toast;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ValidarOtpActivity extends AppCompatActivity {

    /** Executor para trabajar en hilo secundario **/
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_validar_otp);
        EditText etCodigoOtp = findViewById(R.id.etCodigoOtp);
        Button btnConfirmarOtp = findViewById(R.id.btnConfirmarOtp);
        TextView tvOtpEmail = findViewById(R.id.tvOtpEmail);

        String email = getIntent().getStringExtra("EMAIL");

        if (email == null) {
            finish();
            return;
        }

        tvOtpEmail.setText("Enviamos un código a " + email);

/**
        btnConfirmarOtp.setOnClickListener(view -> {
            String codigoIngresado =
                    etCodigoOtp.getText().toString().trim();

            if (codigoIngresado.isEmpty()) {
                etCodigoOtp.setError("Ingresá el código");
                return;
            }

            if (codigoIngresado.length() != 6) {
                etCodigoOtp.setError("El código debe tener 6 dígitos");
                return;
            }

            if (!codigoIngresado.equals(OTP_VALIDO)) {
                etCodigoOtp.setError("El código es incorrecto");
                return;
            }

            Intent intent = new Intent(
                    ValidarOtpActivity.this,
                    MainActivity.class
            );

            startActivity(intent);
        });

**/
/**
        TextView tvOtpEmail = findViewById(R.id.tvOtpEmail);

        String email = getIntent().getStringExtra("EMAIL");

        if (email == null) {
            finish();
            return;
        }

        tvOtpEmail.setText("Enviamos un código a " + email);**/

        btnConfirmarOtp.setOnClickListener(view -> {
            String codigoIngresado =
                    etCodigoOtp.getText().toString().trim();

            if (codigoIngresado.isEmpty()) {
                etCodigoOtp.setError("Ingresá el código");
                return;
            }

            if (!codigoIngresado.matches("\\d{6}")) {
                etCodigoOtp.setError(
                        "El código debe tener 6 dígitos"
                );
                return;
            }

            btnConfirmarOtp.setEnabled(false);

            executor.execute(() -> {
                try {
                    ApiClient.LoginResult result =
                            ApiClient.verifyLoginOtp(
                                    email,
                                    codigoIngresado
                            );

                    runOnUiThread(() -> {
                        btnConfirmarOtp.setEnabled(true);

                        if (result.statusCode() == 200
                                && result.token() != null) {

                            getSharedPreferences(
                                    "ronda_session",
                                    MODE_PRIVATE
                            ).edit()
                                    .putString(
                                            "access_token",
                                            result.token()
                                    )
                                    .apply();

                            Intent intent = new Intent(
                                    ValidarOtpActivity.this,
                                    MainActivity.class
                            );

                            startActivity(intent);
                            finish();
                        } else {
                            etCodigoOtp.setError(
                                    result.message()
                            );
                        }
                    });
                } catch (Exception error) {
                    Log.e(
                            "ValidarOtpActivity",
                            "Error al validar OTP",
                            error
                    );

                    runOnUiThread(() -> {
                        btnConfirmarOtp.setEnabled(true);

                        Toast.makeText(
                                ValidarOtpActivity.this,
                                "No se pudo validar el código",
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }
            });
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    /** este onDestroy sirve para detener el ExecutorService en caso de que el usuario
     * salga de la pantalla en medio de la peticion
     */
    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}