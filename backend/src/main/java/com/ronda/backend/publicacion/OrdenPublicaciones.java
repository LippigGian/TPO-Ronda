package com.ronda.backend.publicacion;

import org.springframework.data.domain.Sort;

/** Criterios de ordenamiento disponibles en el Home. */
public enum OrdenPublicaciones {
    RECIENTES(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
    PRECIO_ASC(Sort.by(Sort.Order.asc("precio"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
    PRECIO_DESC(Sort.by(Sort.Order.desc("precio"), Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

    private final Sort sort;

    OrdenPublicaciones(Sort sort) {
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }
}
