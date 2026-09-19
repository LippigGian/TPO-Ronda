package com.example.ronda;

import android.content.Context;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Single;

public final class PublicacionesCacheManager {

    private static final int MAX_DETALLES_CACHEADOS = 20;

    private static final Preferences.Key<String> HOME_JSON = PreferencesKeys.stringKey("home_json");
    private static final Preferences.Key<Long> HOME_TIMESTAMP = PreferencesKeys.longKey("home_timestamp");
    private static final Preferences.Key<String> DETALLES_JSON = PreferencesKeys.stringKey("detalles_json");

    private static PublicacionesCacheManager instance;

    private final RxDataStore<Preferences> dataStore;
    private final Gson gson = new Gson();

    private PublicacionesCacheManager(Context context) {
        dataStore = new RxPreferenceDataStoreBuilder(
                context.getApplicationContext(),
                "publicaciones_cache"
        ).build();
    }

    public static synchronized PublicacionesCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new PublicacionesCacheManager(context);
        }
        return instance;
    }

    /** Se llama cada vez que el Home carga bien desde el servidor. **/
    public void guardarHome(List<ApiClient.PublicacionResponse> publicaciones) {
        String json = gson.toJson(publicaciones);
        dataStore.updateDataAsync(preferencias -> {
            MutablePreferences editables = preferencias.toMutablePreferences();
            editables.set(HOME_JSON, json);
            editables.set(HOME_TIMESTAMP, System.currentTimeMillis());
            return Single.just(editables);
        }).subscribe();
    }

    /** Lee el último listado guardado, para mostrarlo sin conexión. **/
    public void leerHome(Consumer<HomeCache> alLeer) {
        dataStore.data()
                .firstOrError()
                .map(preferencias -> {
                    String json = preferencias.get(HOME_JSON);
                    Long timestamp = preferencias.get(HOME_TIMESTAMP);

                    if (json == null) {
                        return new HomeCache(new ArrayList<>(), 0L);
                    }

                    Type tipoLista = new TypeToken<List<ApiClient.PublicacionResponse>>() {}.getType();
                    List<ApiClient.PublicacionResponse> lista = gson.fromJson(json, tipoLista);
                    return new HomeCache(lista != null ? lista : new ArrayList<>(),
                            timestamp != null ? timestamp : 0L);
                })
                .subscribe(alLeer::accept, error -> alLeer.accept(new HomeCache(new ArrayList<>(), 0L)));
    }

    /** Se llama cada vez que el usuario abre el detalle de una publicación (para poder reverla offline). **/
    public void guardarDetalle(ApiClient.PublicacionResponse publicacion) {
        dataStore.data()
                .firstOrError()
                .map(this::leerListaDetalles)
                .subscribe(detalles -> {
                    detalles.removeIf(p -> p.getId() == publicacion.getId());
                    detalles.add(0, publicacion);
                    while (detalles.size() > MAX_DETALLES_CACHEADOS) {
                        detalles.remove(detalles.size() - 1);
                    }

                    String json = gson.toJson(detalles);
                    dataStore.updateDataAsync(preferencias -> {
                        MutablePreferences editables = preferencias.toMutablePreferences();
                        editables.set(DETALLES_JSON, json);
                        return Single.just(editables);
                    }).subscribe();
                }, error -> { });
    }

    /** Busca el detalle cacheado de una publicación puntual (para abrirla offline). **/
    public void leerDetalle(long id, Consumer<ApiClient.PublicacionResponse> alLeer) {
        dataStore.data()
                .firstOrError()
                .map(this::leerListaDetalles)
                .subscribe(detalles -> {
                    for (ApiClient.PublicacionResponse publicacion : detalles) {
                        if (publicacion.getId() == id) {
                            alLeer.accept(publicacion);
                            return;
                        }
                    }
                    alLeer.accept(null);
                }, error -> alLeer.accept(null));
    }

    private List<ApiClient.PublicacionResponse> leerListaDetalles(Preferences preferencias) {
        String json = preferencias.get(DETALLES_JSON);
        if (json == null) {
            return new ArrayList<>();
        }
        Type tipoLista = new TypeToken<List<ApiClient.PublicacionResponse>>() {}.getType();
        List<ApiClient.PublicacionResponse> lista = gson.fromJson(json, tipoLista);
        return lista != null ? lista : new ArrayList<>();
    }

    public static final class HomeCache {
        public final List<ApiClient.PublicacionResponse> publicaciones;
        public final long timestamp;

        public HomeCache(List<ApiClient.PublicacionResponse> publicaciones, long timestamp) {
            this.publicaciones = publicaciones;
            this.timestamp = timestamp;
        }
    }
}