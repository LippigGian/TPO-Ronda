package com.ronda.backend.oferta;

/** Estados posibles de una oferta. Solo PENDIENTE admite respuestas; el resto son finales. */
public enum EstadoOferta {
    PENDIENTE("pendiente"),
    ACEPTADA("aceptada"),
    RECHAZADA("rechazada"),
    VENCIDA("vencida");

    private final String descripcion;

    EstadoOferta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
