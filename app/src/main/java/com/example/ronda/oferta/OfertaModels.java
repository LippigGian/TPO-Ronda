package com.example.ronda.oferta;

import java.util.Collections;
import java.util.List;

/**
 * Modelos JSON de ofertas. Los nombres de los campos coinciden con OfertaDtos del backend,
 * porque Gson los mapea por nombre.
 */
public final class OfertaModels {
    public static final String PENDIENTE = "PENDIENTE";
    public static final String ACEPTADA = "ACEPTADA";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String VENCIDA = "VENCIDA";
    public static final String COMPRADOR = "COMPRADOR";

    private OfertaModels() { }

    /** Oferta inicial o contraoferta: un precio y un mensaje opcional. */
    public static final class PropuestaRequest {
        private final double precio;
        private final String mensaje;

        public PropuestaRequest(double precio, String mensaje) {
            this.precio = precio;
            this.mensaje = mensaje;
        }
    }

    public static final class PublicacionResumen {
        private long id;
        private String titulo;
        private double precioPublicado;
        private String foto;
        private String estadoPublicacion;

        public long getId() { return id; }
        public String getTitulo() { return titulo; }
        public double getPrecioPublicado() { return precioPublicado; }
        public String getFoto() { return foto; }
        public String getEstadoPublicacion() { return estadoPublicacion; }
    }

    /** La otra persona de la negociación (vendedor si soy comprador, y viceversa). */
    public static final class Contraparte {
        private long id;
        private String nombre;

        public long getId() { return id; }
        public String getNombre() { return nombre; }
    }

    /** Punto de entrega: el backend lo manda solo cuando la oferta está aceptada. */
    public static final class Entrega {
        private String direccion;
        private double latitud;
        private double longitud;

        public String getDireccion() { return direccion; }
        public double getLatitud() { return latitud; }
        public double getLongitud() { return longitud; }
    }

    public static final class Oferta {
        private long id;
        private PublicacionResumen publicacion;
        private String miRol;
        private Contraparte contraparte;
        private double precio;
        private String mensaje;
        private String estado;
        private String turno;
        private boolean puedoResponder;
        private String venceAt;
        private String createdAt;
        private String updatedAt;
        private Entrega entrega;

        public long getId() { return id; }
        public PublicacionResumen getPublicacion() { return publicacion; }
        public String getMiRol() { return miRol; }
        public Contraparte getContraparte() { return contraparte; }
        /** Última propuesta: la oferta inicial o la última contraoferta. */
        public double getPrecio() { return precio; }
        public String getMensaje() { return mensaje; }
        public String getEstado() { return estado; }
        public String getTurno() { return turno; }
        /** true si la oferta está pendiente y me toca responder a mí. */
        public boolean isPuedoResponder() { return puedoResponder; }
        public String getVenceAt() { return venceAt; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public Entrega getEntrega() { return entrega; }

        public boolean soyComprador() { return COMPRADOR.equals(miRol); }
        public boolean estaPendiente() { return PENDIENTE.equals(estado); }
        public boolean estaAceptada() { return ACEPTADA.equals(estado); }
    }

    public static final class MisOfertas {
        private List<Oferta> enviadas;
        private List<Oferta> recibidas;
        private long pendientesDeMiRespuesta;

        public List<Oferta> getEnviadas() { return enviadas != null ? enviadas : Collections.emptyList(); }
        public List<Oferta> getRecibidas() { return recibidas != null ? recibidas : Collections.emptyList(); }
        public long getPendientesDeMiRespuesta() { return pendientesDeMiRespuesta; }
    }
}
