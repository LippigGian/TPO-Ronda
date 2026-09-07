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
public class ValidarOtpActivity extends AppCompatActivity {
    private static final String OTP_VALIDO = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_validar_otp);
        EditText etCodigoOtp = findViewById(R.id.etCodigoOtp);
        Button btnConfirmarOtp = findViewById(R.id.btnConfirmarOtp);

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


        TextView tvOtpEmail = findViewById(R.id.tvOtpEmail);

        String email = getIntent().getStringExtra("EMAIL");

        if (email == null) {
            finish();
            return;
        }

        tvOtpEmail.setText("Enviamos un código a " + email);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}