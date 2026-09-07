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
            Toast.makeText(
                    SolicitarOtpActivity.this,
                    "Código de prueba: 123456",
                    Toast.LENGTH_LONG
            ).show();
            Intent intent = new Intent(
                    SolicitarOtpActivity.this,
                    ValidarOtpActivity.class
            );

            intent.putExtra("EMAIL", email);

            startActivity(intent);

/*            Toast.makeText(
                    SolicitarOtpActivity.this,
                    "Código enviado a " + email,
                    Toast.LENGTH_SHORT
            ).show();*/
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }
}