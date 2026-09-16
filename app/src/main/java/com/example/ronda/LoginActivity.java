package com.example.ronda;

import android.os.Bundle;
import android.util.Log;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class LoginActivity extends AppCompatActivity {
    /** Declaro executor para poder utilizar otro hilo que no sea el principal **/
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

            btnLogin.setEnabled(false);
            /** De aqui en adelante todo se realizará desde un hilo secundario **/
            executor.execute(() -> {
                try {
                    ApiClient.LoginResult result = ApiClient.login(email, password);
                    /**Una vez que tengo respuesta vuelvo al hilo proicnipal **/
                    runOnUiThread(() -> {
                        btnLogin.setEnabled(true);
                        if (result.statusCode() == 200 && result.token() != null) {
                            getSharedPreferences("ronda_session", MODE_PRIVATE).edit()
                                    .putString("access_token", result.token()).apply();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    result.statusCode() == 401 || result.statusCode() == 403 ? "Email o contraseña incorrectos" : result.message(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception error) {
                    Log.e("LoginActivity", "Error al iniciar sesión contra el backend", error);
                    runOnUiThread(() -> {
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this,
                                "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show();
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
