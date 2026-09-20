package com.ronda.backend.oferta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Se ejecuta con mvn -Pintegration verify. Usa demo@ronda.com (V6) como comprador y vendedor@ronda.com (V11).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OfertaIT {
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
    void negociacionCompletaHastaVerLaDireccion() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 100000);

        // 1. El comprador ofrece un precio distinto al publicado.
        var oferta = api.ofertar(comprador, publicacionId, 80000);
        assertThat(oferta.statusCode()).isEqualTo(201);
        assertThat(oferta.body()).contains("\"estado\":\"PENDIENTE\"", "\"miRol\":\"COMPRADOR\"",
                "\"turno\":\"VENDEDOR\"", "\"puedoResponder\":false");
        long ofertaId = OfertaTestClient.idDe(oferta);

        // 2. Todavía no ve la dirección exacta.
        assertThat(api.get("/api/v1/publicaciones/" + publicacionId, comprador).body())
                .contains("\"direccionVisible\":false");

        // 3. El vendedor la ve en "recibidas" y contraoferta.
        assertThat(api.get("/api/v1/ofertas/mias", vendedor).body()).contains("\"id\":" + ofertaId);
        var contraoferta = api.post("/api/v1/ofertas/" + ofertaId + "/contraofertar", vendedor,
                "{\"precio\":90000,\"mensaje\":\"Te lo dejo en 90 mil\"}");
        assertThat(contraoferta.statusCode()).isEqualTo(200);
        assertThat(contraoferta.body()).contains("\"turno\":\"COMPRADOR\"", "\"miRol\":\"VENDEDOR\"");

        // 4. El comprador acepta la contraoferta y recién ahí recibe el punto de entrega.
        var aceptada = api.postSinCuerpo("/api/v1/ofertas/" + ofertaId + "/aceptar", comprador);
        assertThat(aceptada.statusCode()).isEqualTo(200);
        assertThat(aceptada.body()).contains("\"estado\":\"ACEPTADA\"", "\"direccion\":\"Gorriti 4500, Palermo\"");
        assertThat(api.get("/api/v1/publicaciones/" + publicacionId, comprador).body())
                .contains("\"direccionVisible\":true", "\"direccion\":\"Gorriti 4500, Palermo\"");
    }

    @Test
    void noSePuedeOfertarEnUnaPublicacionPropia() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 50000);

        var response = api.ofertar(vendedor, publicacionId, 40000);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("propia publicación");
    }

    @Test
    void laOfertaDebeTenerUnPrecioDistintoAlPublicado() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 50000);

        var response = api.ofertar(comprador, publicacionId, 50000);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("precio distinto al publicado");
    }

    @Test
    void unPrecioInvalidoDevuelveUnMensajeClaro() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 50000);

        var response = api.post("/api/v1/publicaciones/" + publicacionId + "/ofertas", comprador, "{\"precio\":0}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("El precio debe ser mayor a 0");
    }

    @Test
    void noSePuedenTenerDosOfertasPendientesEnLaMismaPublicacion() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 50000);
        api.ofertar(comprador, publicacionId, 40000);

        assertThat(api.ofertar(comprador, publicacionId, 45000).statusCode()).isEqualTo(409);
    }

    @Test
    void soloQuienTieneElTurnoPuedeResponder() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 50000);
        long ofertaId = OfertaTestClient.idDe(api.ofertar(comprador, publicacionId, 40000));

        var response = api.postSinCuerpo("/api/v1/ofertas/" + ofertaId + "/aceptar", comprador);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("esperar la respuesta");
    }

    @Test
    void unaOfertaRechazadaYaNoSePuedeAceptar() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 50000);
        long ofertaId = OfertaTestClient.idDe(api.ofertar(comprador, publicacionId, 40000));

        var rechazada = api.postSinCuerpo("/api/v1/ofertas/" + ofertaId + "/rechazar", vendedor);
        assertThat(rechazada.body()).contains("\"estado\":\"RECHAZADA\"");

        assertThat(api.postSinCuerpo("/api/v1/ofertas/" + ofertaId + "/aceptar", vendedor).statusCode())
                .isEqualTo(409);
    }

    @Test
    void conUnaOfertaAceptadaNoSeAdmitenNuevasOfertas() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 50000);
        long ofertaId = OfertaTestClient.idDe(api.ofertar(comprador, publicacionId, 40000));
        api.postSinCuerpo("/api/v1/ofertas/" + ofertaId + "/aceptar", vendedor);

        var response = api.ofertar(comprador, publicacionId, 45000);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("oferta aceptada");
    }

    @Test
    void unaOfertaAjenaNoSeRevela() throws Exception {
        assertThat(api.get("/api/v1/ofertas/999999", comprador).statusCode()).isEqualTo(404);
    }

    @Test
    void lasOfertasRequierenSesion() throws Exception {
        assertThat(api.sinSesion("/api/v1/ofertas/mias").statusCode()).isEqualTo(403);
    }
}
