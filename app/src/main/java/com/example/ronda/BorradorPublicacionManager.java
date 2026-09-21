package com.example.ronda;

import android.content.Context;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Single;

public final class BorradorPublicacionManager {
    private static final Preferences.Key<String> TITULO = PreferencesKeys.stringKey("titulo");
    private static final Preferences.Key<String> DESCRIPCION = PreferencesKeys.stringKey("descripcion");
    private static final Preferences.Key<String> CATEGORIA = PreferencesKeys.stringKey("categoria");
    private static final Preferences.Key<String> PRECIO = PreferencesKeys.stringKey("precio");
    private static final Preferences.Key<String> DIRECCION = PreferencesKeys.stringKey("direccion");
    private static final Preferences.Key<String> LATITUD = PreferencesKeys.stringKey("latitud");
    private static final Preferences.Key<String> LONGITUD = PreferencesKeys.stringKey("longitud");
    private static final Preferences.Key<String> ESTADO_ARTICULO = PreferencesKeys.stringKey("estado_articulo");

    private static BorradorPublicacionManager instance;

    private final RxDataStore<Preferences> dataStore;

    private BorradorPublicacionManager(Context context) {
        dataStore = new RxPreferenceDataStoreBuilder(
                context.getApplicationContext(),
                "borrador_publicacion"
        ).build();
    }

    public static synchronized BorradorPublicacionManager getInstance(Context context) {
        if (instance == null) {
            instance = new BorradorPublicacionManager(context);
        }
        return instance;
    }

    public void guardar(Borrador borrador) {
        dataStore.updateDataAsync(preferencias -> {
            MutablePreferences editables = preferencias.toMutablePreferences();
            editables.set(TITULO, borrador.titulo);
            editables.set(DESCRIPCION, borrador.descripcion);
            editables.set(CATEGORIA, borrador.categoria);
            editables.set(PRECIO, borrador.precio);
            editables.set(DIRECCION, borrador.direccion);
            editables.set(LATITUD, borrador.latitud);
            editables.set(LONGITUD, borrador.longitud);
            editables.set(ESTADO_ARTICULO, borrador.estadoArticulo);
            return Single.just(editables);
        }).subscribe();
    }

    public void leer(Consumer<Borrador> alLeer) {
        dataStore.data()
                .firstOrError()
                .map(preferencias -> new Borrador(
                        valor(preferencias, TITULO),
                        valor(preferencias, DESCRIPCION),
                        valor(preferencias, CATEGORIA),
                        valor(preferencias, PRECIO),
                        valor(preferencias, DIRECCION),
                        valor(preferencias, LATITUD),
                        valor(preferencias, LONGITUD),
                        valor(preferencias, ESTADO_ARTICULO)
                ))
                .subscribe(alLeer::accept, error -> { });
    }

    public void borrar() {
        dataStore.updateDataAsync(preferencias -> {
            MutablePreferences editables = preferencias.toMutablePreferences();
            editables.clear();
            return Single.just(editables);
        }).subscribe();
    }

    private static String valor(Preferences preferencias, Preferences.Key<String> clave) {
        String valor = preferencias.get(clave);
        return valor == null ? "" : valor;
    }

    public static final class Borrador {
        public final String titulo;
        public final String descripcion;
        public final String categoria;
        public final String precio;
        public final String direccion;
        public final String latitud;
        public final String longitud;
        public final String estadoArticulo;

        public Borrador(
                String titulo,
                String descripcion,
                String categoria,
                String precio,
                String direccion,
                String latitud,
                String longitud,
                String estadoArticulo
        ) {
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.categoria = categoria;
            this.precio = precio;
            this.direccion = direccion;
            this.latitud = latitud;
            this.longitud = longitud;
            this.estadoArticulo = estadoArticulo;
        }

        public boolean estaVacio() {
            return titulo.isEmpty()
                    && descripcion.isEmpty()
                    && categoria.isEmpty()
                    && precio.isEmpty()
                    && direccion.isEmpty()
                    && latitud.isEmpty()
                    && longitud.isEmpty();
        }
    }
}
