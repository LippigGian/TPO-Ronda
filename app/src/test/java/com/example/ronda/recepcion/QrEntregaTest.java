package com.example.ronda.recepcion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class QrEntregaTest {

    @Test
    public void parseaElJsonDelQrDeEntrega() throws Exception {
        QrEntrega qr = QrEntrega.parsear(
                "{\"tipo\": \"RONDA-ENTREGA\", \"publicacionId\": 12, \"titulo\": \"Bicicleta rodado 26\"}");

        assertEquals(12L, qr.getPublicacionId());
        assertEquals("Bicicleta rodado 26", qr.getTitulo());
    }

    @Test
    public void toleraEspaciosYSaltosDeLineaAlrededor() throws Exception {
        QrEntrega qr = QrEntrega.parsear("  {\"tipo\":\"RONDA-ENTREGA\",\"publicacionId\":7}\n");

        assertEquals(7L, qr.getPublicacionId());
        assertNull(qr.getTitulo());
    }

    @Test
    public void elContenidoGeneradoSePuedeVolverAParsear() throws Exception {
        String contenido = QrEntrega.contenido(5, "Silla \"vintage\"");

        QrEntrega qr = QrEntrega.parsear(contenido);

        assertEquals(5L, qr.getPublicacionId());
        assertEquals("Silla \"vintage\"", qr.getTitulo());
    }

    @Test
    public void rechazaElFormatoViejoDeTextoSimple() {
        assertInvalido("RONDA-ENTREGA-12");
    }

    @Test
    public void rechazaTextosQueNoSonJson() {
        assertInvalido("https://www.ejemplo.com");
        assertInvalido("{no es json");
        assertInvalido("[1, 2]");
        assertInvalido("");
        assertInvalido(null);
    }

    @Test
    public void rechazaJsonDeOtroTipo() {
        assertInvalido("{\"socioId\": 482, \"sede\": \"Palermo\", \"clase\": \"Funcional 18:00\"}");
        assertInvalido("{\"tipo\": \"OTRA-COSA\", \"publicacionId\": 12}");
    }

    @Test
    public void rechazaPublicacionIdFaltanteOInvalido() {
        assertInvalido("{\"tipo\": \"RONDA-ENTREGA\"}");
        assertInvalido("{\"tipo\": \"RONDA-ENTREGA\", \"publicacionId\": \"12\"}");
        assertInvalido("{\"tipo\": \"RONDA-ENTREGA\", \"publicacionId\": 12.5}");
        assertInvalido("{\"tipo\": \"RONDA-ENTREGA\", \"publicacionId\": 0}");
        assertInvalido("{\"tipo\": \"RONDA-ENTREGA\", \"publicacionId\": -3}");
    }

    private static void assertInvalido(String crudo) {
        assertThrows(QrEntrega.QrInvalidoException.class, () -> QrEntrega.parsear(crudo));
    }
}
