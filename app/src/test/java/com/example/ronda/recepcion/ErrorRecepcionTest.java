package com.example.ronda.recepcion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ErrorRecepcionTest {

    @Test
    public void sesionVencidaCuandoElBackendRespondeSinCuerpo() {
        assertEquals(ErrorRecepcion.Accion.INICIAR_SESION, ErrorRecepcion.desdeRespuesta(403, true).getAccion());
        assertEquals(ErrorRecepcion.Accion.INICIAR_SESION, ErrorRecepcion.desdeRespuesta(401, false).getAccion());
        assertEquals("Sesión vencida", ErrorRecepcion.desdeRespuesta(403, true).getTitulo());
    }

    @Test
    public void unForbiddenConDetalleEsQueNoEsTuCompra() {
        ErrorRecepcion error = ErrorRecepcion.desdeRespuesta(403, false);

        assertEquals("No podés confirmar esta entrega", error.getTitulo());
        assertEquals(ErrorRecepcion.Accion.VOLVER, error.getAccion());
    }

    @Test
    public void yaConfirmadaSoloOfreceVolver() {
        ErrorRecepcion error = ErrorRecepcion.desdeRespuesta(409, false);

        assertEquals("Recepción ya confirmada", error.getTitulo());
        assertEquals(ErrorRecepcion.Accion.VOLVER, error.getAccion());
    }

    @Test
    public void publicacionInexistenteSeMuestraComoQrInvalidoSinDarDetalles() {
        assertEquals(ErrorRecepcion.qrInvalido().getMensaje(), ErrorRecepcion.desdeRespuesta(404, false).getMensaje());
        assertEquals(ErrorRecepcion.qrInvalido().getMensaje(), ErrorRecepcion.desdeRespuesta(400, false).getMensaje());
    }

    @Test
    public void erroresDelServidorPermitenReintentar() {
        ErrorRecepcion error = ErrorRecepcion.desdeRespuesta(500, false);

        assertEquals("No se pudo confirmar", error.getTitulo());
        assertEquals(ErrorRecepcion.Accion.ESCANEAR_DE_NUEVO, error.getAccion());
    }

    @Test
    public void ningunMensajeMuestraCodigosNiDetallesTecnicos() {
        int[] codigos = {400, 401, 403, 404, 409, 500, 502, 503};
        for (int codigo : codigos) {
            for (boolean vacio : new boolean[]{true, false}) {
                String texto = ErrorRecepcion.desdeRespuesta(codigo, vacio).getTitulo() + " "
                        + ErrorRecepcion.desdeRespuesta(codigo, vacio).getMensaje();
                assertFalse(texto, texto.matches(".*\\d{3}.*"));
                assertFalse(texto, texto.toLowerCase().contains("error"));
            }
        }
    }

    @Test
    public void otraPublicacionNombraSoloLaQueSeEstaRecibiendo() {
        String mensaje = ErrorRecepcion.otraPublicacion("Auriculares").getMensaje();

        assertEquals("El QR no corresponde a esta publicación. Escaneá el QR de entrega de «Auriculares».", mensaje);
    }
}
