package com.ronda.backend.publicacion;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Arma la consulta del Home combinando (AND) solo los filtros que llegaron. */
final class PublicacionSpecs {
    private static final double RADIO_TIERRA_KM = 6371.0;

    private PublicacionSpecs() { }

    static Specification<Publicacion> explorar(ExplorarFiltros filtros) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(root.get("estadoPublicacion"), EstadoPublicacion.ACTIVA));

            if (filtros.q() != null && !filtros.q().isBlank()) {
                String patron = "%" + escapar(filtros.q().trim().toLowerCase()) + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.get("titulo")), patron, '\\'),
                        cb.like(cb.lower(root.get("descripcion")), patron, '\\')));
            }
            if (filtros.categoria() != null && !filtros.categoria().isBlank()) {
                condiciones.add(cb.equal(cb.lower(root.get("categoria")), filtros.categoria().trim().toLowerCase()));
            }
            if (filtros.precioMin() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("precio"), filtros.precioMin()));
            }
            if (filtros.precioMax() != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.get("precio"), filtros.precioMax()));
            }
            if (filtros.estadoArticulo() != null) {
                condiciones.add(cb.equal(root.get("estadoArticulo"), filtros.estadoArticulo()));
            }
            if (filtros.filtraPorCercania()) {
                // Haversine: d <= r  <=>  a <= sin^2(r / 2R), así se evita acos/asin en la base.
                Expression<Double> lat = cb.function("radians", Double.class, root.get("latitud"));
                Expression<Double> lng = cb.function("radians", Double.class, root.get("longitud"));
                double lat0 = Math.toRadians(filtros.lat());
                double lng0 = Math.toRadians(filtros.lng());

                Expression<Double> senoLat = cb.function("sin", Double.class,
                        cb.quot(cb.diff(lat, lat0), 2.0));
                Expression<Double> senoLng = cb.function("sin", Double.class,
                        cb.quot(cb.diff(lng, lng0), 2.0));
                Expression<Double> cosenoLat = cb.function("cos", Double.class, lat);

                Expression<Double> a = cb.sum(
                        cb.prod(senoLat, senoLat),
                        cb.prod(cb.prod(cosenoLat, Math.cos(lat0)), cb.prod(senoLng, senoLng)));
                double seno = Math.sin(filtros.radioKm() / (2 * RADIO_TIERRA_KM));
                condiciones.add(cb.lessThanOrEqualTo(a, seno * seno));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
