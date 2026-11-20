package com.ronda.backend.operacion;

import com.ronda.backend.oferta.OfertaTestClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Se ejecuta con mvn -Pintegration verify. Cierre de la operacion escaneando el QR fisico de entrega:
// la app parsea el JSON del QR y envia el publicacionId.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfirmarRecepcionIT {
    private static final String CONFIRMAR = "/api/v1/operaciones/confirmar-recepcion";

    @Value("${local.server.port}")
    int port;

    OfertaTestClient api;
    String comprador;
    String vendedor;

    @BeforeEach
    void iniciarSesiones() throws Exception {
        api = new OfertaTestClient(port);
        comprador = api.login(OfertaTestClient.COMPRADOR);
        vendedor = api.login(OfertaTestClient.VENDEDOR);
    }

    @Test
    void elCompradorEscaneaElQrYLaOperacionQuedaCerradaParaAmbos() throws Exception {
        long publicacionId = publicacionConOfertaAceptada(80000);

        var response = confirmar(comprador, publicacionId);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("\"tipo\":\"COMPRA\"", "\"publicacionId\":" + publicacionId,
                "\"montoFinal\":80000", "\"yaCalificada\":false");

        // La publicacion pasa sola a VENDIDA.
        assertThat(api.get("/api/v1/publicaciones/" + publicacionId, vendedor).body())
                .contains("\"estadoPublicacion\":\"VENDIDA\"");

        // La operacion queda registrada para las dos partes y habilita la calificacion.
        assertThat(api.get("/api/v1/operaciones/historial?tipo=COMPRA", comprador).body())
                .contains("\"publicacionId\":" + publicacionId);
        assertThat(api.get("/api/v1/operaciones/historial?tipo=VENTA", vendedor).body())
                .contains("\"publicacionId\":" + publicacionId);
        assertThat(api.get("/api/v1/operaciones/pendientes-calificar", vendedor).body())
                .contains("\"publicacionId\":" + publicacionId);
    }

    @Test
    void escanearDosVecesElMismoQrAvisaQueYaEstaConfirmada() throws Exception {
        long publicacionId = publicacionConOfertaAceptada(80000);
        assertThat(confirmar(comprador, publicacionId).statusCode()).isEqualTo(201);

        var response = confirmar(comprador, publicacionId);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("Ya confirmaste la recepción");
    }

    @Test
    void unPedidoSinPublicacionEsInvalido() throws Exception {
        var response = api.post(CONFIRMAR, comprador, "{}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("el QR no indica la publicación");
    }

    @Test
    void unQrDeUnaPublicacionInexistenteEsInvalido() throws Exception {
        var response = confirmar(comprador, 999999999L);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("no corresponde a ninguna publicación");
    }

    @Test
    void sinOfertaAceptadaNoSePuedeConfirmar() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 100000);
        assertThat(api.ofertar(comprador, publicacionId, 80000).statusCode()).isEqualTo(201);

        var response = confirmar(comprador, publicacionId);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("no corresponde a una compra tuya");
        assertThat(api.get("/api/v1/publicaciones/" + publicacionId, vendedor).body())
                .contains("\"estadoPublicacion\":\"ACTIVA\"");
    }

    @Test
    void elVendedorNoPuedeConfirmarSuPropiaVenta() throws Exception {
        long publicacionId = publicacionConOfertaAceptada(80000);

        var response = confirmar(vendedor, publicacionId);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void elVendedorYaNoPuedeMarcarLaVentaAMano() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 100000);

        var response = patchEstado(vendedor, publicacionId, "VENDIDA");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(api.get("/api/v1/publicaciones/" + publicacionId, vendedor).body())
                .contains("\"estadoPublicacion\":\"ACTIVA\"");
    }

    @Test
    void requiereSesion() throws Exception {
        var response = send(HttpRequest.newBuilder(uri(CONFIRMAR))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"publicacionId\":1}")));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    private long publicacionConOfertaAceptada(int precioOferta) throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 100000);
        var oferta = api.ofertar(comprador, publicacionId, precioOferta);
        assertThat(oferta.statusCode()).isEqualTo(201);
        var aceptada = api.postSinCuerpo("/api/v1/ofertas/" + OfertaTestClient.idDe(oferta) + "/aceptar", vendedor);
        assertThat(aceptada.statusCode()).isEqualTo(200);
        return publicacionId;
    }

    private HttpResponse<String> confirmar(String token, long publicacionId) throws Exception {
        return api.post(CONFIRMAR, token, "{\"publicacionId\":" + publicacionId + "}");
    }

    private HttpResponse<String> patchEstado(String token, long publicacionId, String estado) throws Exception {
        return send(HttpRequest.newBuilder(uri("/api/v1/publicaciones/" + publicacionId + "/estado"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"estado\":\"" + estado + "\"}")));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            return client.send(request.timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
