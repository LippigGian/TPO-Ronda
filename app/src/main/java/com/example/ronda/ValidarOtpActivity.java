package com.example.ronda;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class ValidarOtpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_validar_otp);

        EditText etCodigoOtp = findViewById(R.id.etCodigoOtp);
        TextInputLayout tilCodigoOtp = findViewById(R.id.tilCodigoOtp);
        Button btnConfirmarOtp = findViewById(R.id.btnConfirmarOtp);
        Button btnReenviarOtp = findViewById(R.id.btnReenviarOtp);
        TextView tvOtpEmail = findViewById(R.id.tvOtpEmail);

        String email = getIntent().getStringExtra("EMAIL");
        String purposeReceived = getIntent().getStringExtra("OTP_PURPOSE");
        final String otpPurpose = purposeReceived == null ? "LOGIN" : purposeReceived;

        if (email == null) {
            finish();
            return;
        }
        tvOtpEmail.setText("Enviamos un código a " + email);

        /**Reenviar codigo OTP **/
        btnReenviarOtp.setOnClickListener(view -> {
            btnReenviarOtp.setEnabled(false);

            ApiClient.api()
                    .resendOtp(
                            new ApiClient.OtpRequest(
                                    email,
                                    otpPurpose
                            )
                    )
                    .enqueue(
                            new Callback<ApiClient.OtpRequestResponse>() {
                                @Override
                                public void onResponse(
                                        Call<ApiClient.OtpRequestResponse> call,
                                        Response<ApiClient.OtpRequestResponse> response
                                ) {
                                    btnReenviarOtp.setEnabled(true);

                                    if (response.code() == 202) {
                                        etCodigoOtp.setText("");

                                        Toast.makeText(
                                                ValidarOtpActivity.this,
                                                "Enviamos un nuevo código",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    } else {
                                        Toast.makeText(
                                                ValidarOtpActivity.this,
                                                ApiClient.errorMessage(
                                                        response,
                                                        "No se pudo reenviar el código"
                                                ),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }

                                @Override
                                public void onFailure(
                                        Call<ApiClient.OtpRequestResponse> call,
                                        Throwable error
                                ) {
                                    btnReenviarOtp.setEnabled(true);

                                    Log.e(
                                            "ValidarOtpActivity",
                                            "Error al reenviar OTP",
                                            error
                                    );

                                    Toast.makeText(
                                            ValidarOtpActivity.this,
                                            "No se pudo conectar con el servidor",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                    );
        });

        /** Confirmar  enviar codigo OTP**/
        btnConfirmarOtp.setOnClickListener(view -> {
            String code = etCodigoOtp.getText().toString().trim();
            if (!code.matches("\\d{6}")) {
                tilCodigoOtp.setError("El código debe tener 6 dígitos");
                return;
            }
            tilCodigoOtp.setError(null);

            btnConfirmarOtp.setEnabled(false);
            ApiClient.api().verifyOtp(new ApiClient.OtpVerifyRequest(email, otpPurpose, code))
                    .enqueue(new Callback<ApiClient.LoginResponse>() {
                        @Override
                        public void onResponse(Call<ApiClient.LoginResponse> call,
                                               Response<ApiClient.LoginResponse> response) {
                            btnConfirmarOtp.setEnabled(true);
                            ApiClient.LoginResponse body = response.body();

                            if (response.isSuccessful() && body != null && body.getToken() != null) {
                                new SessionManager(ValidarOtpActivity.this).saveToken(body.getToken());
                                startActivity(new Intent(ValidarOtpActivity.this, MainActivity.class));
                                finish();
                                return;
                            }

                            tilCodigoOtp.setError(
                                    ApiClient.errorMessage(response, "No se pudo validar el código")
                            );
                        }

                        @Override
                        public void onFailure(Call<ApiClient.LoginResponse> call, Throwable error) {
                            btnConfirmarOtp.setEnabled(true);
                            Log.e("ValidarOtpActivity", "Error al validar OTP", error);
                            Toast.makeText(ValidarOtpActivity.this,
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
