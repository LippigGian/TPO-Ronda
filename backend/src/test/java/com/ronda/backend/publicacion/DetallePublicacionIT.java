package com.ronda.backend.publicacion;

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

// Se ejecuta con mvn -Pintegration verify. Usa las publicaciones de prueba de V11 (vendedor@ronda.com).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DetallePublicacionIT {
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
    void elDetalleDeUnaPublicacionAjenaOcultaLaDireccionExacta() throws Exception {
        long id = idDe("Notebook Lenovo");

        var response = get("/api/v1/publicaciones/" + id);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"titulo\":\"Notebook Lenovo 14\\\"\"", "\"descripcion\"", "\"fotos\"",
                "\"esPropia\":false", "\"direccionVisible\":false", "\"direccion\":null", "\"latitud\":null",
                "\"longitud\":null", "\"nombre\":\"Vendedor demo\"", "\"reputacion\"", "\"miembroDesde\"");
    }

    @Test
    void elDuenoVeLaDireccionExactaDeSuPublicacion() throws Exception {
        var crear = send(request("/api/v1/publicaciones").header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"titulo":"Mesa de luz","descripcion":"Madera clara","categoria":"Hogar","precio":15000,
                         "estadoArticulo":"USADO","direccion":"Thames 1500, Palermo","latitud":-34.5900,"longitud":-58.4300}
                        """)));
        assertThat(crear.statusCode()).isEqualTo(201);
        Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(crear.body());
        assertThat(matcher.find()).isTrue();

        var response = get("/api/v1/publicaciones/" + matcher.group(1));

        assertThat(response.body()).contains("\"esPropia\":true", "\"direccionVisible\":true",
                "\"direccion\":\"Thames 1500, Palermo\"", "\"zona\":\"Palermo\"");
    }

    @Test
    void unaPublicacionInexistenteDevuelve404() throws Exception {
        assertThat(get("/api/v1/publicaciones/999999").statusCode()).isEqualTo(404);
    }

    @Test
    void elDetalleRequiereSesion() throws Exception {
        assertThat(send(request("/api/v1/publicaciones/1").GET()).statusCode()).isEqualTo(403);
    }

    private long idDe(String titulo) throws Exception {
        var lista = get("/api/v1/publicaciones?size=50");
        Matcher matcher = Pattern.compile("\"id\":(\\d+),\"titulo\":\"" + Pattern.quote(titulo)).matcher(lista.body());
        assertThat(matcher.find()).as("publicación de prueba %s", titulo).isTrue();
        return Long.parseLong(matcher.group(1));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return send(request(path).header("Authorization", "Bearer " + token).GET());
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
