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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SolicitarOtpActivity extends AppCompatActivity {
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
            ApiClient.api().requestOtp(new ApiClient.OtpRequest(email, "LOGIN"))
                    .enqueue(new Callback<ApiClient.OtpRequestResponse>() {
                        @Override
                        public void onResponse(Call<ApiClient.OtpRequestResponse> call,
                                               Response<ApiClient.OtpRequestResponse> response) {
                            btnEnviarOtp.setEnabled(true);
                            if (response.code() == 202) {
                                Intent intent = new Intent(
                                        SolicitarOtpActivity.this,
                                        ValidarOtpActivity.class
                                );
                                intent.putExtra("EMAIL", email);
                                startActivity(intent);
                                return;
                            }

                            Toast.makeText(SolicitarOtpActivity.this,
                                    ApiClient.errorMessage(response, "No se pudo solicitar el código"),
                                    Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(Call<ApiClient.OtpRequestResponse> call, Throwable error) {
                            btnEnviarOtp.setEnabled(true);
                            Log.e("SolicitarOtpActivity", "Error al solicitar OTP", error);
                            Toast.makeText(SolicitarOtpActivity.this,
                                    "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show();
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
