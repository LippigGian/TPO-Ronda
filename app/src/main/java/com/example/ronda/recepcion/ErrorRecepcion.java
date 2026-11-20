package com.example.ronda.recepcion;

/**
 * Mensajes de error del cierre con QR. Al usuario solo se le dice lo mínimo para que sepa qué hacer:
 * nunca códigos HTTP, detalles técnicos ni datos de otras publicaciones.
 */
public final class ErrorRecepcion {

    /** Qué ofrece el botón principal del panel de error. */
    public enum Accion { ESCANEAR_DE_NUEVO, INICIAR_SESION, VOLVER }

    private final String titulo;
    private final String mensaje;
    private final Accion accion;

    private ErrorRecepcion(String titulo, String mensaje, Accion accion) {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.accion = accion;
    }

    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public Accion getAccion() { return accion; }

    // ---------- Errores detectados en la app, antes de llamar al servidor ----------

    /** No es JSON, no es de Ronda o le falta la publicación. */
    public static ErrorRecepcion qrInvalido() {
        return new ErrorRecepcion("Código inválido",
                "Este QR no es válido. Escaneá el QR de entrega que te dio el vendedor.",
                Accion.ESCANEAR_DE_NUEVO);
    }

    /** QR de Ronda, pero de otra publicación: no se dice de cuál. */
    public static ErrorRecepcion otraPublicacion(String tituloEsperado) {
        return new ErrorRecepcion("Código inválido",
                "El QR no corresponde a esta publicación. Escaneá el QR de entrega de «" + tituloEsperado + "».",
                Accion.ESCANEAR_DE_NUEVO);
    }

    public static ErrorRecepcion sinConexion() {
        return new ErrorRecepcion("Sin conexión",
                "Conectate a internet y probá de nuevo.",
                Accion.ESCANEAR_DE_NUEVO);
    }

    // ---------- Respuestas del servidor ----------

    /**
     * @param codigo     código HTTP de la respuesta.
     * @param cuerpoVacio el backend responde 403 sin cuerpo cuando el token venció o falta
     *                    (SecurityConfig); los 403 de negocio traen un "detail".
     */
    public static ErrorRecepcion desdeRespuesta(int codigo, boolean cuerpoVacio) {
        if (codigo == 401 || (codigo == 403 && cuerpoVacio)) {
            return new ErrorRecepcion("Sesión vencida",
                    "Volvé a iniciar sesión para confirmar la recepción.",
                    Accion.INICIAR_SESION);
        }
        switch (codigo) {
            case 400:
            case 404:
                return qrInvalido();
            case 403:
                return new ErrorRecepcion("No podés confirmar esta entrega",
                        "Solo el comprador con la oferta aceptada puede confirmar la recepción.",
                        Accion.VOLVER);
            case 409:
                return new ErrorRecepcion("Recepción ya confirmada",
                        "Ya confirmaste la recepción de este artículo.",
                        Accion.VOLVER);
            default:
                return new ErrorRecepcion("No se pudo confirmar",
                        "Probá de nuevo en unos minutos.",
                        Accion.ESCANEAR_DE_NUEVO);
        }
    }

    /** El servidor no respondió (sin red, servidor caído, timeout). */
    public static ErrorRecepcion servidorInaccesible() {
        return new ErrorRecepcion("Sin conexión",
                "No pudimos conectarnos. Revisá tu conexión y probá de nuevo.",
                Accion.ESCANEAR_DE_NUEVO);
    }
}
