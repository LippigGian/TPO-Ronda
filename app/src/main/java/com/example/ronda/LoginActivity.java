package com.example.ronda;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//Importo el intent para poder comunicarme con MainActivity. //
import android.content.Intent;


public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //Set content view enlaza o asocia la logica con la vista
        setContentView(R.layout.activity_login);
        //findViewById() busca el componente creado desde el XML y nos permite manejarlo desde Java.
        //Los nombres son los ids que pusimos en activity_login.xml
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnOtp = findViewById(R.id.btnOtp);

        //Logica listener boton OTP
        btnOtp.setOnClickListener(view ->{
            Intent intent = new Intent(
                    LoginActivity.this,
                    SolicitarOtpActivity.class
            );
                    startActivity(intent);
        });


        //Logica listener boton login
        btnLogin.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            if (email.isEmpty()) {
                etEmail.setError("Ingresá tu correo electrónico");
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Ingresá tu contraseña");
                return;
            }

            Toast.makeText(
                    LoginActivity.this,
                    "Datos ingresados correctamente",
                    Toast.LENGTH_SHORT
            ).show();
            //Creo el intent:
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}