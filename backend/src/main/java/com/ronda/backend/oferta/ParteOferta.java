package com.ronda.backend.oferta;

/** Las dos partes de una negociacion. Se usa para el rol de quien mira y para el turno. */
public enum ParteOferta {
    COMPRADOR,
    VENDEDOR;

    /** La contraparte: si responde el vendedor, el turno pasa al comprador y viceversa. */
    public ParteOferta otra() {
        return this == COMPRADOR ? VENDEDOR : COMPRADOR;
    }
}
