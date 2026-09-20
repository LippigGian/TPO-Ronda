package com.example.ronda.favorito;

/**
 * Modelos JSON de favoritos y búsquedas guardadas. Los nombres de los campos coinciden
 * con FavoritoDtos/BusquedaGuardadaDtos del backend, porque Gson los mapea por nombre.
 */
public final class FavoritoModels {
    private FavoritoModels() { }

    public static final class FavoritoItem {
        private long publicacionId;
        private String titulo;
        private String foto;
        private double precioActual;
        private double precioGuardado;
        private boolean cambioPrecio;
        private String estadoPublicacion;
        private String createdAt;

        public long getPublicacionId() { return publicacionId; }
        public String getTitulo() { return titulo; }
        public String getFoto() { return foto; }
        public double getPrecioActual() { return precioActual; }
        public double getPrecioGuardado() { return precioGuardado; }
        /** true si el precio cambió desde que se agregó (o desde la última vez que se vio el detalle). */
        public boolean isCambioPrecio() { return cambioPrecio; }
        public String getEstadoPublicacion() { return estadoPublicacion; }
        public String getCreatedAt() { return createdAt; }
    }

    /** Guarda los mismos filtros combinables del Home (espejo de FiltrosHome), más un nombre. */
    public static final class GuardarBusquedaRequest {
        private final String nombre;
        private final String q;
        private final String categoria;
        private final Double precioMin;
        private final Double precioMax;
        private final String estadoArticulo;
        private final Double lat;
        private final Double lng;
        private final Double radioKm;

        public GuardarBusquedaRequest(String nombre, String q, String categoria, Double precioMin,
                                      Double precioMax, String estadoArticulo, Double lat, Double lng,
                                      Double radioKm) {
            this.nombre = nombre;
            this.q = q;
            this.categoria = categoria;
            this.precioMin = precioMin;
            this.precioMax = precioMax;
            this.estadoArticulo = estadoArticulo;
            this.lat = lat;
            this.lng = lng;
            this.radioKm = radioKm;
        }
    }

    public static final class BusquedaGuardadaItem {
        private long id;
        private String nombre;
        private String q;
        private String categoria;
        private Double precioMin;
        private Double precioMax;
        private String estadoArticulo;
        private boolean cercania;
        private Double radioKm;
        private long cantidadNuevas;
        private String createdAt;

        public long getId() { return id; }
        public String getNombre() { return nombre; }
        public String getQ() { return q; }
        public String getCategoria() { return categoria; }
        public Double getPrecioMin() { return precioMin; }
        public Double getPrecioMax() { return precioMax; }
        public String getEstadoArticulo() { return estadoArticulo; }
        /** true si la búsqueda filtra por cercanía (tenía coordenadas al guardarla). */
        public boolean isCercania() { return cercania; }
        public Double getRadioKm() { return radioKm; }
        /** Publicaciones nuevas desde la última vez que se abrió esta búsqueda: el indicador de novedad. */
        public long getCantidadNuevas() { return cantidadNuevas; }
        public String getCreatedAt() { return createdAt; }
    }
}
