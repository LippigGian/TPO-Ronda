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

public class SolicitarOtpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_solicitar_otp);

        EditText etEmailOtp = findViewById(R.id.etEmailOtp);
        TextInputLayout tilEmailOtp = findViewById(R.id.tilEmailOtp);
        Button btnEnviarOtp = findViewById(R.id.btnEnviarOtp);
        /** Recibo y leo el intent enviado desde LoginActivity **/
        String purposeReceived = getIntent().getStringExtra("OTP_PURPOSE");
        /** Si recibe purposeReceived utiliza ese, si es NULL utiliza login **/
        final String otpPurpose = purposeReceived == null ? "LOGIN" : purposeReceived;


        btnEnviarOtp.setOnClickListener(view -> {
            String email = etEmailOtp.getText().toString().trim();
            if (email.isEmpty()) {
                tilEmailOtp.setError("Ingresá tu correo electrónico");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmailOtp.setError("Ingresá un correo electrónico válido");
                return;
            }
            tilEmailOtp.setError(null);

            btnEnviarOtp.setEnabled(false);

            ApiClient.api().requestOtp(new ApiClient.OtpRequest(email, otpPurpose))
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
                                intent.putExtra("OTP_PURPOSE", otpPurpose);
                                startActivity(intent);
                                return;
                            }

                            String message = response.code() == 404
                                    ? "No existe una cuenta con ese email"
                                    : ApiClient.errorMessage(response, "No se pudo solicitar el código");

                            tilEmailOtp.setError(message);
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
