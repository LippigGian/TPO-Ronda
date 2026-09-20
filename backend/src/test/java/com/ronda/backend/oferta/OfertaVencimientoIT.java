package com.ronda.backend.oferta;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Se ejecuta con mvn -Pintegration verify. Con vigencia 0 minutos, toda oferta nace ya vencida.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.ofertas.vigencia-minutos=0")
class OfertaVencimientoIT {
    @Value("${local.server.port}")
    int port;

    @Test
    void unaOfertaFueraDePlazoCaducaSolaYNoAdmiteRespuestas() throws Exception {
        var api = new OfertaTestClient(port);
        String comprador = api.login(OfertaTestClient.COMPRADOR);
        String vendedor = api.login(OfertaTestClient.VENDEDOR);
        long publicacionId = api.crearPublicacion(vendedor, 70000);
        long ofertaId = OfertaTestClient.idDe(api.ofertar(comprador, publicacionId, 60000));

        assertThat(api.get("/api/v1/ofertas/" + ofertaId, vendedor).body()).contains("\"estado\":\"VENCIDA\"");

        var response = api.postSinCuerpo("/api/v1/ofertas/" + ofertaId + "/aceptar", vendedor);
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("vencida");
    }
}
