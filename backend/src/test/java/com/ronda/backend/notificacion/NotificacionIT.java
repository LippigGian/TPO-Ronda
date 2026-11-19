package com.ronda.backend.notificacion;

import com.ronda.backend.oferta.OfertaTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Punto 11. Se ejecuta con mvn -Pintegration verify, igual que OfertaIT (comparte la base y los usuarios demo).
// Otras pruebas generan avisos para los mismos usuarios, por eso cada chequeo busca su propia oferta.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificacionIT {
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
    void cadaPasoDeLaNegociacionAvisaALaOtraParteUnaSolaVez() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 100000);

        // 1. Oferta: le llega al vendedor, con destino a la oferta.
        long ofertaId = OfertaTestClient.idDe(api.ofertar(comprador, publicacionId, 80000));
        String paraVendedor = api.get("/api/v1/notificaciones/nuevas", vendedor).body();
        assertThat(paraVendedor).contains("\"tipo\":\"OFERTA_RECIBIDA\"", "\"ofertaId\":" + ofertaId,
                "\"publicacionId\":" + publicacionId, "$ 80.000");
        // Ya entregada: no se repite.
        assertThat(api.get("/api/v1/notificaciones/nuevas", vendedor).body()).doesNotContain("\"ofertaId\":" + ofertaId);
        // Quien hizo la accion no recibe aviso.
        assertThat(api.get("/api/v1/notificaciones/nuevas", comprador).body())
                .doesNotContain("\"ofertaId\":" + ofertaId);

        // 2. Contraoferta del vendedor: le llega al comprador.
        api.post("/api/v1/ofertas/" + ofertaId + "/contraofertar", vendedor, "{\"precio\":90000}");
        assertThat(api.get("/api/v1/notificaciones/nuevas", comprador).body())
                .contains("\"tipo\":\"CONTRAOFERTA_RECIBIDA\"", "\"ofertaId\":" + ofertaId, "$ 90.000");

        // 3. El comprador acepta: le llega al vendedor.
        api.postSinCuerpo("/api/v1/ofertas/" + ofertaId + "/aceptar", comprador);
        assertThat(api.get("/api/v1/notificaciones/nuevas", vendedor).body())
                .contains("\"tipo\":\"OFERTA_ACEPTADA\"", "\"ofertaId\":" + ofertaId);
    }

    @Test
    void elRechazoAvisaAlComprador() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 50000);
        long ofertaId = OfertaTestClient.idDe(api.ofertar(comprador, publicacionId, 30000));

        api.postSinCuerpo("/api/v1/ofertas/" + ofertaId + "/rechazar", vendedor);

        assertThat(api.get("/api/v1/notificaciones/nuevas", comprador).body())
                .contains("\"tipo\":\"OFERTA_RECHAZADA\"", "\"ofertaId\":" + ofertaId);
    }

    @Test
    void laBandejaMuestraLosAvisosYPermiteMarcarlosLeidos() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 60000);
        long ofertaId = OfertaTestClient.idDe(api.ofertar(comprador, publicacionId, 55000));

        String bandeja = api.get("/api/v1/notificaciones", vendedor).body();
        assertThat(bandeja).contains("\"ofertaId\":" + ofertaId, "\"leida\":false", "\"noLeidas\":");
        long avisoId = Long.parseLong(bandeja.replaceAll("(?s).*?\"id\":(\\d+),\"tipo\":\"OFERTA_RECIBIDA\"[^}]*\"ofertaId\":"
                + ofertaId + "\\b.*", "$1"));

        assertThat(api.postSinCuerpo("/api/v1/notificaciones/" + avisoId + "/leida", vendedor).statusCode())
                .isEqualTo(204);
        // Un aviso ajeno no se puede marcar.
        assertThat(api.postSinCuerpo("/api/v1/notificaciones/" + avisoId + "/leida", comprador).statusCode())
                .isEqualTo(404);
        assertThat(api.postSinCuerpo("/api/v1/notificaciones/leidas", vendedor).statusCode()).isEqualTo(204);
        assertThat(api.get("/api/v1/notificaciones", vendedor).body()).contains("\"noLeidas\":0");
    }

    @Test
    void sinSesionNoHayAvisos() throws Exception {
        assertThat(api.sinSesion("/api/v1/notificaciones/nuevas").statusCode()).isIn(401, 403);
    }
}
