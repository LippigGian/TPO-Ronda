package com.example.ronda;

import android.app.Application;

import com.example.ronda.notificacion.AvisosPoller;

public class RondaApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.initialize(this);
        // Punto 11: avisos de preguntas y ofertas como notificaciones del sistema.
        AvisosPoller.iniciar(this);
    }
}
