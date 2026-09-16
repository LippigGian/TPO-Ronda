package com.example.ronda;

import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class ApiClient {
    private static final String BASE_URL = "http://127.0.0.1:8080";

    private ApiClient() { }
    /** Declaracion de clase intera **/
    public static final class LoginResult {
        private final int statusCode;
        private final String token;
        private final String message;

        public LoginResult(int statusCode, String token, String message) {
            this.statusCode = statusCode;
            this.token = token;
            this.message = message;
        }

        public int statusCode() {
            return statusCode;
        }

        public String token() {
            return token;
        }

        public String message() {
            return message;
        }
    }
    /** Login por mail y contraseña**/
    public static LoginResult login(String email, String password) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "/api/v1/auth/login").openConnection();
        /** Configuraciones de la peticion **/
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);
        /**Arma el cuerpo JSON de la peticion **/
        byte[] body = new JSONObject().put("email", email).put("password", password)
                .toString().getBytes(StandardCharsets.UTF_8);
        /** Luego se envia al servidor **/
        try (OutputStream output = connection.getOutputStream()) { output.write(body); }
        /** Codigo de la respuesta (400, 401, 404, etc) **/
        int status = connection.getResponseCode();

        var stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        /** Aqui se convierte el contenido de la respuesta recibida en texto**/
        String response = stream == null ? "" : readResponse(stream);
        String token = null;
        String message = "No se pudo iniciar sesion";
        if (!response.trim().isEmpty()) {
            JSONObject json = new JSONObject(response);
            token = json.optString("token", null);
            message = json.optString("detail", json.optString("message", message));
        }
        connection.disconnect();
        /** Finalmente devuelve la respuesta al LoginActivity.java**/

        return new LoginResult(status, token, message);
    }


    /** Declaracion de clase intera **/
    public static final class OtpRequestResult {
        private final int statusCode;
        private final String message;

        public OtpRequestResult(int statusCode, String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        public int statusCode() {
            return statusCode;
        }

        public String message() {
            return message;
        }
    }

    /** Login via OTP **/
    public static OtpRequestResult requestLoginOtp(String email) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                BASE_URL + "/api/v1/auth/otp/request"
        ).openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
        );
        connection.setDoOutput(true);

        byte[] body = new JSONObject()
                .put("email", email)
                .put("purpose", "LOGIN")
                .toString()
                .getBytes(StandardCharsets.UTF_8);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();

        String response = stream == null ? "" : readResponse(stream);
        String message = status == 202
                ? "Código enviado"
                : "No se pudo enviar el código";

        if (!response.trim().isEmpty()) {
            JSONObject json = new JSONObject(response);
            message = json.optString(
                    "detail",
                    json.optString("message", message)
            );
        }

        connection.disconnect();
        return new OtpRequestResult(status, message);
    }

    /**Metodo para validar el codigo **/
    public static LoginResult verifyLoginOtp(
            String email,
            String code
    ) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                BASE_URL + "/api/v1/auth/otp/verify"
        ).openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
        );
        connection.setDoOutput(true);

        byte[] body = new JSONObject()
                .put("email", email)
                .put("purpose", "LOGIN")
                .put("code", code)
                .toString()
                .getBytes(StandardCharsets.UTF_8);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();

        String response = stream == null ? "" : readResponse(stream);
        String token = null;
        String message = "No se pudo validar el código";

        if (!response.trim().isEmpty()) {
            JSONObject json = new JSONObject(response);
            token = json.optString("token", null);
            message = json.optString(
                    "detail",
                    json.optString("message", message)
            );
        }

        connection.disconnect();
        return new LoginResult(status, token, message);
    }

    private static String readResponse(InputStream stream) throws Exception {
        StringBuilder result = new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }
}
