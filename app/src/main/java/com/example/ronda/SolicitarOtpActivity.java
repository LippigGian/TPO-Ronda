package com.example.ronda;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SolicitarOtpActivity extends AppCompatActivity {

    /** Executor para trabajar en hilo secundario **/
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_solicitar_otp);
        EditText etEmailOtp = findViewById(R.id.etEmailOtp);
        Button btnEnviarOtp = findViewById(R.id.btnEnviarOtp);

        btnEnviarOtp.setOnClickListener(view -> {
            String email = etEmailOtp.getText().toString().trim();

            if (email.isEmpty()) {
                etEmailOtp.setError("Ingresá tu correo electrónico");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmailOtp.setError("Ingresá un correo electrónico válido");
                return;
            }
            btnEnviarOtp.setEnabled(false);

            executor.execute(() -> {
                try {
                    ApiClient.OtpRequestResult result =
                            ApiClient.requestLoginOtp(email);

                    runOnUiThread(() -> {
                        btnEnviarOtp.setEnabled(true);

                        if (result.statusCode() == 202) {
                            Intent intent = new Intent(
                                    SolicitarOtpActivity.this,
                                    ValidarOtpActivity.class
                            );

                            intent.putExtra("EMAIL", email);
                            startActivity(intent);
                        } else {
                            Toast.makeText(
                                    SolicitarOtpActivity.this,
                                    result.message(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                } catch (Exception error) {
                    Log.e(
                            "SolicitarOtpActivity",
                            "Error al solicitar OTP",
                            error
                    );

                    runOnUiThread(() -> {
                        btnEnviarOtp.setEnabled(true);

                        Toast.makeText(
                                SolicitarOtpActivity.this,
                                "No se pudo solicitar el código",
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