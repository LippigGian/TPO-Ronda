package com.ronda.backend.perfil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Se ejecuta con mvn -Pintegration verify. Usa el usuario demo (V6) y el vendedor de prueba (V11).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PerfilIT {
    @Value("${local.server.port}")
    int port;

    String token;

    @BeforeEach
    void iniciarSesion() throws Exception {
        var response = send(request("/api/v1/auth/login")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"demo@ronda.com\",\"password\":\"ronda123\"}")));
        Matcher matcher = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(response.body());
        assertThat(matcher.find()).isTrue();
        token = matcher.group(1);
    }

    @Test
    void miPerfilIncluyeEmailYReputacion() throws Exception {
        var response = get("/api/v1/usuarios/me");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"email\":\"demo@ronda.com\"", "\"reputacion\"",
                "\"ventasConcretadas\"", "\"comprasConcretadas\"", "\"miembroDesde\"");
    }

    @Test
    void editarElPerfilGuardaLosDatos() throws Exception {
        var response = put("/api/v1/usuarios/me",
                "{\"email\":\"demo@ronda.com\",\"nombre\":\"Demo Editado\",\"telefono\":\"11 5555 5555\",\"zona\":\"Palermo\"}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(get("/api/v1/usuarios/me").body()).contains("\"nombre\":\"Demo Editado\"",
                "\"telefono\":\"11 5555 5555\"", "\"zona\":\"Palermo\"");
    }

    @Test
    void elEmailEsObligatorio() throws Exception {
        assertThat(put("/api/v1/usuarios/me", "{\"nombre\":\"Demo\"}").statusCode()).isEqualTo(400);
    }

    @Test
    void elNombreEsObligatorio() throws Exception {
        assertThat(put("/api/v1/usuarios/me", "{\"email\":\"demo@ronda.com\",\"nombre\":\"  \"}").statusCode())
                .isEqualTo(400);
    }

    @Test
    void unTelefonoInvalidoSeRechaza() throws Exception {
        assertThat(put("/api/v1/usuarios/me",
                "{\"email\":\"demo@ronda.com\",\"nombre\":\"Demo\",\"telefono\":\"abc\"}").statusCode())
                .isEqualTo(400);
    }

    @Test
    void cambiarElEmailAUnoYaUsadoPorOtraCuentaSeRechaza() throws Exception {
        var response = put("/api/v1/usuarios/me",
                "{\"email\":\"vendedor@ronda.com\",\"nombre\":\"Demo\"}");

        assertThat(response.statusCode()).isEqualTo(409);
        // El email de la cuenta no debe haber cambiado.
        assertThat(get("/api/v1/usuarios/me").body()).contains("\"email\":\"demo@ronda.com\"");
    }

    @Test
    void elPerfilPublicoMuestraReputacionYPublicacionesSinDatosPrivados() throws Exception {
        long vendedorId = vendedorDe("Notebook Lenovo");

        var response = get("/api/v1/usuarios/" + vendedorId + "/perfil-publico");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"nombre\":\"Vendedor demo\"", "\"reputacion\"", "\"miembroDesde\"",
                "\"publicacionesActivas\"", "Bicicleta rodado 29");
        assertThat(response.body()).doesNotContain("vendedor@ronda.com", "\"email\"", "\"telefono\"");
    }

    @Test
    void unUsuarioInexistenteDevuelve404() throws Exception {
        assertThat(get("/api/v1/usuarios/999999/perfil-publico").statusCode()).isEqualTo(404);
    }

    @Test
    void elPerfilRequiereSesion() throws Exception {
        assertThat(send(request("/api/v1/usuarios/me").GET()).statusCode()).isEqualTo(403);
    }

    /** Obtiene el id del vendedor desde el detalle de una publicacion de prueba. */
    private long vendedorDe(String titulo) throws Exception {
        var lista = get("/api/v1/publicaciones?size=50");
        Matcher publicacion = Pattern.compile("\"id\":(\\d+),\"titulo\":\"" + Pattern.quote(titulo)).matcher(lista.body());
        assertThat(publicacion.find()).as("publicación de prueba %s", titulo).isTrue();

        var detalle = get("/api/v1/publicaciones/" + publicacion.group(1));
        Matcher vendedor = Pattern.compile("\"vendedor\":\\{\"id\":(\\d+)").matcher(detalle.body());
        assertThat(vendedor.find()).isTrue();
        return Long.parseLong(vendedor.group(1));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return send(request(path).header("Authorization", "Bearer " + token).GET());
    }

    private HttpResponse<String> put(String path, String json) throws Exception {
        return send(request(path).header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json)));
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            return client.send(request.timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
