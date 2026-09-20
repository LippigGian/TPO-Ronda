package com.ronda.backend.publicacion;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
class ExplorarPublicacionesIT {
    @Value("${local.server.port}")
    int port;

    String token;

    @BeforeEach
    void iniciarSesion() throws Exception {
        var response = send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"demo@ronda.com\",\"password\":\"ronda123\"}")));
        Matcher matcher = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(response.body());
        assertThat(matcher.find()).isTrue();
        token = matcher.group(1);
    }

    @Test
    void elListadoRequiereSesion() throws Exception {
        var response = send(HttpRequest.newBuilder(uri("/api/v1/publicaciones")).GET());
        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void paginaElListadoYNoExponeLaDireccionExacta() throws Exception {
        var response = get("/api/v1/publicaciones?size=3");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"size\":3", "\"totalElements\"", "\"zona\"", "\"last\":false");
        assertThat(response.body()).doesNotContain("\"direccion\"", "\"latitud\"", "\"longitud\"");
    }

    @Test
    void buscaPorTextoEnTituloYDescripcion() throws Exception {
        var porTitulo = get("/api/v1/publicaciones?q=" + encode("notebook"));
        assertThat(porTitulo.body()).contains("Notebook Lenovo").doesNotContain("Bicicleta");

        var porDescripcion = get("/api/v1/publicaciones?q=" + encode("SSD 512"));
        assertThat(porDescripcion.body()).contains("Notebook Lenovo");
    }

    @Test
    void combinaFiltrosYOrdenaPorPrecio() throws Exception {
        var response = get("/api/v1/publicaciones?categoria=hogar&precioMax=220000&orden=PRECIO_ASC");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Cafetera Nespresso", "Mesa de comedor")
                .doesNotContain("Sillón de 3 cuerpos", "Notebook");
        assertThat(response.body().indexOf("Cafetera Nespresso"))
                .isLessThan(response.body().indexOf("Mesa de comedor"));
    }

    @Test
    void filtraPorEstadoDelArticulo() throws Exception {
        var response = get("/api/v1/publicaciones?estadoArticulo=NUEVO");
        assertThat(response.body()).contains("Campera de cuero").doesNotContain("Notebook");
    }

    @Test
    void filtraPorCercaniaYDevuelveLaDistanciaAproximada() throws Exception {
        // Belgrano: cerca la bicicleta y las zapatillas, lejos Flores.
        var response = get("/api/v1/publicaciones?lat=-34.5617&lng=-58.4561&radioKm=3");

        assertThat(response.body()).contains("Bicicleta rodado 29", "Zapatillas running", "\"distanciaKm\":1")
                .doesNotContain("Mesa de comedor");
    }

    @Test
    void listaLasCategoriasDisponibles() throws Exception {
        var response = get("/api/v1/publicaciones/categorias");
        assertThat(response.body()).contains("Hogar", "Tecnología");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).header("Authorization", "Bearer " + token).GET());
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            return client.send(request.timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static String encode(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }
}
