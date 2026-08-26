package com.example.ronda.model;

public class Publicacion {

    private String titulo;
    private double precio;
    private String estado;
    private String zona;

    public Publicacion (String titulo, double precio, String estado, String zona){

        this.titulo = titulo;
        this.precio = precio;
        this.estado = estado;
        this.zona = zona;
    }

    public String getTitulo() {
        return titulo;
    }

    public double getPrecio() {
        return precio;
    }

    public String getEstado() {
        return estado;
    }

    public String getZona() {
        return zona;
    }
}
