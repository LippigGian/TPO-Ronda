package com.ronda.backend.oferta;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** Cliente HTTP compartido por los tests de ofertas, con el mismo estilo que los IT del resto del equipo. */
final class OfertaTestClient {
    static final String COMPRADOR = "demo@ronda.com";
    static final String VENDEDOR = "vendedor@ronda.com";
    private static final String PASSWORD = "ronda123";

    private final int port;

    OfertaTestClient(int port) {
        this.port = port;
    }

    String login(String email) throws Exception {
        var response = send(request("/api/v1/auth/login").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")));
        return primerGrupo("\"token\":\"([^\"]+)\"", response.body());
    }

    /** Publicacion nueva por test: asi cada test es independiente de los demas. */
    long crearPublicacion(String token, int precio) throws Exception {
        var response = post("/api/v1/publicaciones", token, """
                {"titulo":"Artículo para ofertas","descripcion":"Prueba de negociación","categoria":"Hogar",
                 "precio":%d,"estadoArticulo":"USADO","direccion":"Gorriti 4500, Palermo",
                 "latitud":-34.5900,"longitud":-58.4300}
                """.formatted(precio));
        assertThat(response.statusCode()).isEqualTo(201);
        return Long.parseLong(primerGrupo("\"id\":(\\d+)", response.body()));
    }

    HttpResponse<String> ofertar(String token, long publicacionId, int precio) throws Exception {
        return post("/api/v1/publicaciones/" + publicacionId + "/ofertas", token,
                "{\"precio\":" + precio + ",\"mensaje\":\"¿Lo dejás en este precio?\"}");
    }

    /** El primer "id" del JSON de una oferta es el de la oferta (primer campo del record). */
    static long idDe(HttpResponse<String> response) {
        return Long.parseLong(primerGrupo("\"id\":(\\d+)", response.body()));
    }

    HttpResponse<String> get(String path, String token) throws Exception {
        return send(request(path).header("Authorization", "Bearer " + token).GET());
    }

    HttpResponse<String> post(String path, String token, String json) throws Exception {
        return send(request(path).header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)));
    }

    HttpResponse<String> postSinCuerpo(String path, String token) throws Exception {
        return send(request(path).header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()));
    }

    HttpResponse<String> sinSesion(String path) throws Exception {
        return send(request(path).GET());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            return client.send(request.timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private static String primerGrupo(String regex, String texto) {
        Matcher matcher = Pattern.compile(regex).matcher(texto);
        assertThat(matcher.find()).as("patrón %s en %s", regex, texto).isTrue();
        return matcher.group(1);
    }
}
