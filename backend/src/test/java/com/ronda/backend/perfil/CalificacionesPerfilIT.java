package com.ronda.backend.perfil;

import com.ronda.backend.oferta.OfertaTestClient;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Se ejecuta con mvn -Pintegration verify. Las calificaciones recibidas se ven en el perfil publico (punto 9).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CalificacionesPerfilIT {
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
    void laCalificacionRecibidaApareceEnElPerfilPublicoDelVendedor() throws Exception {
        long operacionId = operacionCerradaConQr();
        String comentario = "Excelente vendedor " + System.nanoTime();

        var calificacion = api.post("/api/v1/operaciones/" + operacionId + "/calificaciones", comprador,
                "{\"puntaje\":4,\"comentario\":\"" + comentario + "\"}");
        assertThat(calificacion.statusCode()).isEqualTo(201);

        var perfil = api.get("/api/v1/usuarios/" + idDe(OfertaTestClient.VENDEDOR) + "/perfil-publico", comprador);

        assertThat(perfil.statusCode()).isEqualTo(200);
        assertThat(perfil.body()).contains("\"calificaciones\":[", "\"comentario\":\"" + comentario + "\"",
                "\"puntaje\":4", "\"rolAutor\":\"COMPRADOR\"", "\"publicacionTitulo\":\"Artículo para ofertas\"");
        // El perfil publico nunca expone el email de quien califico.
        assertThat(perfil.body()).doesNotContain(OfertaTestClient.COMPRADOR);
    }

    private long operacionCerradaConQr() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 100000);
        var oferta = api.ofertar(comprador, publicacionId, 80000);
        assertThat(oferta.statusCode()).isEqualTo(201);
        assertThat(api.postSinCuerpo("/api/v1/ofertas/" + OfertaTestClient.idDe(oferta) + "/aceptar", vendedor)
                .statusCode()).isEqualTo(200);

        var operacion = api.post("/api/v1/operaciones/confirmar-recepcion", comprador,
                "{\"codigo\":\"RONDA-ENTREGA-" + publicacionId + "\"}");
        assertThat(operacion.statusCode()).isEqualTo(201);
        return OfertaTestClient.idDe(operacion);
    }

    private long idDe(String email) throws Exception {
        var response = api.get("/api/v1/usuarios/buscar?email=" + email, comprador);
        Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(response.body());
        assertThat(matcher.find()).as("id de %s en %s", email, response.body()).isTrue();
        return Long.parseLong(matcher.group(1));
    }
}
