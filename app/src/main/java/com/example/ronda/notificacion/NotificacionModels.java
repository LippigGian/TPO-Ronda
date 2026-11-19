package com.example.ronda.notificacion;

/** Modelos JSON de avisos (punto 11). Mismos nombres que NotificacionDtos del backend. */
public final class NotificacionModels {
    private NotificacionModels() { }

    public static final class Aviso {
        private long id;
        private String tipo;
        private String titulo;
        private String mensaje;
        private Long publicacionId;
        private Long ofertaId;

        public long getId() { return id; }
        public String getTipo() { return tipo; }
        public String getTitulo() { return titulo; }
        public String getMensaje() { return mensaje; }
        public Long getPublicacionId() { return publicacionId; }
        public Long getOfertaId() { return ofertaId; }
    }
}
