package com.example.ronda.pregunta;

/** Modelos JSON de preguntas. Mismos nombres que PreguntaDtos del backend. */
public final class PreguntaModels {
    private PreguntaModels() { }

    public static final class Pregunta {
        private long id;
        private String texto;
        private String autor;
        private boolean esMia;
        private String respuesta;
        private String createdAt;
        private String respondidaAt;
        private boolean puedoResponder;

        public long getId() { return id; }
        public String getTexto() { return texto; }
        public String getAutor() { return autor; }
        public boolean isEsMia() { return esMia; }
        public String getRespuesta() { return respuesta; }
        public String getCreatedAt() { return createdAt; }
        public String getRespondidaAt() { return respondidaAt; }
        public boolean isPuedoResponder() { return puedoResponder; }
    }

    public static final class PreguntarRequest {
        private final String texto;

        public PreguntarRequest(String texto) { this.texto = texto; }
    }

    public static final class ResponderRequest {
        private final String respuesta;

        public ResponderRequest(String respuesta) { this.respuesta = respuesta; }
    }
}
