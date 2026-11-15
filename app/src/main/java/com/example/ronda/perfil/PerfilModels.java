package com.example.ronda.perfil;

import java.util.List;

/**
 * Modelos JSON del perfil. Los nombres de los campos tienen que coincidir
 * exactamente con los del backend (PerfilDtos.java), porque Gson los mapea por nombre.
 */
public final class PerfilModels {
    private PerfilModels() { }

    public static final class ActualizarPerfilRequest {
        private final String email;
        private final String nombre;
        private final String telefono;
        private final String zona;

        public ActualizarPerfilRequest(String email, String nombre, String telefono, String zona) {
            this.email = email;
            this.nombre = nombre;
            this.telefono = telefono;
            this.zona = zona;
        }
    }

    /** Respuesta de PUT /me. token viene con valor solo si el email cambio (ver PerfilDtos.ActualizarPerfilResponse). */
    public static final class ActualizarPerfilResponse {
        private MiPerfil perfil;
        private String token;

        public MiPerfil getPerfil() { return perfil; }
        public String getToken() { return token; }
    }

    public static final class Reputacion {
        private Double promedioEstrellas; // null si todavia no tiene calificaciones
        private long cantidadCalificaciones;
        private long ventasConcretadas;
        private long comprasConcretadas;

        public Double getPromedioEstrellas() { return promedioEstrellas; }
        public long getCantidadCalificaciones() { return cantidadCalificaciones; }
        public long getVentasConcretadas() { return ventasConcretadas; }
        public long getComprasConcretadas() { return comprasConcretadas; }
    }

    public static final class MiPerfil {
        private long id;
        private String email;
        private String nombre;
        private String telefono;
        private String zona;
        private String foto;
        private String miembroDesde;
        private Reputacion reputacion;

        public long getId() { return id; }
        public String getEmail() { return email; }
        public String getNombre() { return nombre; }
        public String getTelefono() { return telefono; }
        public String getZona() { return zona; }
        public String getFoto() { return foto; }
        public String getMiembroDesde() { return miembroDesde; }
        public Reputacion getReputacion() { return reputacion; }
    }

    public static final class PerfilPublico {
        private long id;
        private String nombre;
        private String zona;
        private String foto;
        private String miembroDesde;
        private Reputacion reputacion;
        private List<PublicacionResumen> publicacionesActivas;
        private List<CalificacionRecibida> calificaciones;

        public long getId() { return id; }
        public String getNombre() { return nombre; }
        public String getZona() { return zona; }
        public String getFoto() { return foto; }
        public String getMiembroDesde() { return miembroDesde; }
        public Reputacion getReputacion() { return reputacion; }
        public List<PublicacionResumen> getPublicacionesActivas() { return publicacionesActivas; }
        /** Últimas calificaciones recibidas, de la más nueva a la más vieja (punto 9). */
        public List<CalificacionRecibida> getCalificaciones() { return calificaciones; }
    }

    /** Una calificación que recibió el usuario. rolAutor: COMPRADOR o VENDEDOR (lo que fue quien calificó). */
    public static final class CalificacionRecibida {
        private int puntaje;
        private String comentario;
        private String autorNombre;
        private String rolAutor;
        private String publicacionTitulo;
        private String fecha;

        public int getPuntaje() { return puntaje; }
        public String getComentario() { return comentario; }
        public String getAutorNombre() { return autorNombre; }
        public String getRolAutor() { return rolAutor; }
        public String getPublicacionTitulo() { return publicacionTitulo; }
        public String getFecha() { return fecha; }
    }

    public static final class PublicacionResumen {
        private long id;
        private String titulo;
        private double precio;
        private String estadoArticulo;
        private String foto;

        public long getId() { return id; }
        public String getTitulo() { return titulo; }
        public double getPrecio() { return precio; }
        public String getEstadoArticulo() { return estadoArticulo; }
        public String getFoto() { return foto; }
    }
}
