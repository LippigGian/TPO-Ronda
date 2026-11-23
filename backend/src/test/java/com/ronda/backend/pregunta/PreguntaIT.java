package com.ronda.backend.pregunta;

import com.ronda.backend.oferta.OfertaTestClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Puntos 4 y 11. Se ejecuta con mvn -Pintegration verify, igual que OfertaIT y NotificacionIT.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PreguntaIT {
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
    void preguntarYResponderAvisaALaOtraParte() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 40000);

        // 1. El interesado pregunta: queda en la publicación y le avisa al vendedor.
        HttpResponse<String> creada = api.post("/api/v1/publicaciones/" + publicacionId + "/preguntas", comprador,
                "{\"texto\":\"  ¿Hacés envíos a Palermo?  \"}");
        assertThat(creada.statusCode()).isEqualTo(201);
        assertThat(creada.body()).contains("\"texto\":\"¿Hacés envíos a Palermo?\"", "\"esMia\":true",
                "\"puedoResponder\":false", "\"respuesta\":null");
        long preguntaId = OfertaTestClient.idDe(creada);

        assertThat(api.get("/api/v1/notificaciones/nuevas", vendedor).body())
                .contains("\"tipo\":\"PREGUNTA_RECIBIDA\"", "\"publicacionId\":" + publicacionId,
                        "¿Hacés envíos a Palermo?");

        // El vendedor la ve y la puede responder; el interesado no.
        assertThat(api.get("/api/v1/publicaciones/" + publicacionId + "/preguntas", vendedor).body())
                .contains("\"id\":" + preguntaId, "\"puedoResponder\":true", "\"esMia\":false");
        assertThat(api.post("/api/v1/preguntas/" + preguntaId + "/respuesta", comprador,
                "{\"respuesta\":\"Sí\"}").statusCode()).isEqualTo(403);

        // 2. El vendedor responde: le avisa a quien preguntó.
        HttpResponse<String> respondida = api.post("/api/v1/preguntas/" + preguntaId + "/respuesta", vendedor,
                "{\"respuesta\":\"Sí, coordinamos por la app.\"}");
        assertThat(respondida.statusCode()).isEqualTo(200);
        assertThat(respondida.body()).contains("\"respuesta\":\"Sí, coordinamos por la app.\"",
                "\"puedoResponder\":false");
        assertThat(api.get("/api/v1/notificaciones/nuevas", comprador).body())
                .contains("\"tipo\":\"PREGUNTA_RESPONDIDA\"", "\"publicacionId\":" + publicacionId,
                        "Sí, coordinamos por la app.");

        // Una sola respuesta por pregunta, visible para todos.
        assertThat(api.post("/api/v1/preguntas/" + preguntaId + "/respuesta", vendedor,
                "{\"respuesta\":\"Otra\"}").statusCode()).isEqualTo(409);
        assertThat(api.get("/api/v1/publicaciones/" + publicacionId + "/preguntas", comprador).body())
                .contains("\"respuesta\":\"Sí, coordinamos por la app.\"");
    }

    @Test
    void reglasDeValidacion() throws Exception {
        long publicacionId = api.crearPublicacion(vendedor, 15000);
        String ruta = "/api/v1/publicaciones/" + publicacionId + "/preguntas";

        // El vendedor no se pregunta a sí mismo, y la pregunta no puede estar vacía.
        assertThat(api.post(ruta, vendedor, "{\"texto\":\"¿Sigue disponible?\"}").statusCode()).isEqualTo(400);
        assertThat(api.post(ruta, comprador, "{\"texto\":\"   \"}").statusCode()).isEqualTo(400);
        assertThat(api.post(ruta, comprador, "{\"texto\":\"" + "a".repeat(501) + "\"}").statusCode()).isEqualTo(400);
        assertThat(api.post("/api/v1/publicaciones/999999999/preguntas", comprador,
                "{\"texto\":\"¿Sigue disponible?\"}").statusCode()).isEqualTo(404);
        assertThat(api.post("/api/v1/preguntas/999999999/respuesta", vendedor,
                "{\"respuesta\":\"Sí\"}").statusCode()).isEqualTo(404);
        assertThat(api.sinSesion(ruta).statusCode()).isIn(401, 403);
    }
}
