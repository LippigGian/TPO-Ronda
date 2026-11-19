package com.example.ronda;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Single;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Punto 6: copia local de lo que la persona vio, para poder verlo sin conexión.
 *
 * - El último listado del Home cargado con éxito.
 * - Las últimas publicaciones abiertas (detalle, precio, datos del vendedor) y sus fotos
 *   (las fotos quedan en el caché de disco de Glide).
 *
 * La copia es por usuario y, al recuperar la conexión, se actualiza sola con los datos del servidor.
 */
public final class PublicacionesCacheManager {

    private static final int MAX_DETALLES_CACHEADOS = 20;
    private static final int TAMANO_PAGINA_HOME = 10;

    private static PublicacionesCacheManager instance;

    private final Context appContext;
    private final RxDataStore<Preferences> dataStore;
    private final SessionManager sesion;
    private final Gson gson = new Gson();
    private final Type tipoLista = new TypeToken<List<ApiClient.PublicacionResponse>>() {}.getType();
    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());
    private boolean ultimoOnline;
    private boolean sincronizando;

    private PublicacionesCacheManager(Context context) {
        appContext = context.getApplicationContext();
        dataStore = new RxPreferenceDataStoreBuilder(appContext, "publicaciones_cache").build();
        sesion = new SessionManager(appContext);
        escucharReconexion();
    }

    public static synchronized PublicacionesCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new PublicacionesCacheManager(context);
        }
        return instance;
    }

    // ---------- Home ----------

    /** Se llama cada vez que el Home carga bien desde el servidor. **/
    public void guardarHome(List<ApiClient.PublicacionResponse> publicaciones) {
        Preferences.Key<String> claveJson = claveHome();
        Preferences.Key<Long> claveFecha = claveHomeFecha();
        String json = gson.toJson(publicaciones);
        dataStore.updateDataAsync(preferencias -> {
            MutablePreferences editables = preferencias.toMutablePreferences();
            editables.set(claveJson, json);
            editables.set(claveFecha, System.currentTimeMillis());
            return Single.just(editables);
        }).subscribe(ok -> { }, error -> { });
    }

    /** Lee el último listado guardado, para mostrarlo sin conexión. **/
    public void leerHome(Consumer<HomeCache> alLeer) {
        Preferences.Key<String> claveJson = claveHome();
        Preferences.Key<Long> claveFecha = claveHomeFecha();
        dataStore.data()
                .firstOrError()
                .map(preferencias -> {
                    Long timestamp = preferencias.get(claveFecha);
                    return new HomeCache(leerLista(preferencias.get(claveJson)), timestamp != null ? timestamp : 0L);
                })
                .subscribe(alLeer::accept, error -> alLeer.accept(new HomeCache(new ArrayList<>(), 0L)));
    }

    // ---------- Detalles ----------

    /** Se llama cada vez que el usuario abre el detalle de una publicación: queda primera en la lista. **/
    public void guardarDetalle(ApiClient.PublicacionResponse publicacion) {
        modificarDetalles(detalles -> {
            detalles.removeIf(p -> p.getId() == publicacion.getId());
            detalles.add(0, publicacion);
            while (detalles.size() > MAX_DETALLES_CACHEADOS) {
                detalles.remove(detalles.size() - 1);
            }
        });
        precargarFotos(publicacion);
    }

    /** Busca el detalle cacheado de una publicación puntual (para abrirla offline). **/
    public void leerDetalle(long id, Consumer<ApiClient.PublicacionResponse> alLeer) {
        Preferences.Key<String> clave = claveDetalles();
        dataStore.data()
                .firstOrError()
                .map(preferencias -> leerLista(preferencias.get(clave)))
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

    // ---------- Actualización automática al recuperar la conexión ----------

    /** Vuelve a pedir al servidor el listado del Home y cada publicación guardada, y actualiza la copia. */
    public void sincronizar() {
        if (sincronizando || sesion.getToken() == null || !ConnectivityObserver.isOnline(appContext)) {
            return;
        }
        sincronizando = true;

        ApiClient.api().explorarPublicaciones(null, null, null, null, null, null, null, null,
                "RECIENTES", 0, TAMANO_PAGINA_HOME).enqueue(
                new Callback<ApiClient.PageResponse<ApiClient.PublicacionResponse>>() {
                    @Override
                    public void onResponse(Call<ApiClient.PageResponse<ApiClient.PublicacionResponse>> call,
                                           Response<ApiClient.PageResponse<ApiClient.PublicacionResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            guardarHome(response.body().getContent());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiClient.PageResponse<ApiClient.PublicacionResponse>> call,
                                          Throwable error) { }
                });

        Preferences.Key<String> clave = claveDetalles();
        dataStore.data()
                .firstOrError()
                .map(preferencias -> leerLista(preferencias.get(clave)))
                .subscribe(detalles -> hiloPrincipal.post(() -> {
                    for (ApiClient.PublicacionResponse guardada : detalles) {
                        refrescarDetalle(guardada.getId());
                    }
                    sincronizando = false;
                }), error -> hiloPrincipal.post(() -> sincronizando = false));
    }

    private void refrescarDetalle(long id) {
        ApiClient.api().getPublicacion(id).enqueue(new Callback<ApiClient.PublicacionResponse>() {
            @Override
            public void onResponse(Call<ApiClient.PublicacionResponse> call,
                                   Response<ApiClient.PublicacionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiClient.PublicacionResponse actual = response.body();
                    // Se reemplaza en su lugar: actualizar no cambia el orden de "vistas recientemente".
                    modificarDetalles(detalles -> {
                        for (int i = 0; i < detalles.size(); i++) {
                            if (detalles.get(i).getId() == id) {
                                detalles.set(i, actual);
                            }
                        }
                    });
                    precargarFotos(actual);
                } else if (response.code() == 404 || response.code() == 403) {
                    // Ya no existe o dejó de estar disponible: no se sigue mostrando sin conexión.
                    modificarDetalles(detalles -> detalles.removeIf(p -> p.getId() == id));
                }
            }

            @Override
            public void onFailure(Call<ApiClient.PublicacionResponse> call, Throwable error) { }
        });
    }

    private void escucharReconexion() {
        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        ultimoOnline = ConnectivityObserver.isOnline(appContext);
        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                hiloPrincipal.post(PublicacionesCacheManager.this::revisarConexion);
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                hiloPrincipal.post(PublicacionesCacheManager.this::revisarConexion);
            }

            @Override
            public void onLost(Network network) {
                hiloPrincipal.post(PublicacionesCacheManager.this::revisarConexion);
            }
        });
    }

    private void revisarConexion() {
        boolean online = ConnectivityObserver.isOnline(appContext);
        boolean recupero = online && !ultimoOnline;
        ultimoOnline = online;
        if (recupero) {
            sincronizar();
        }
    }

    // ---------- Helpers ----------

    /** Lectura y escritura en un solo paso: dos cambios simultáneos no se pisan. */
    private void modificarDetalles(Consumer<List<ApiClient.PublicacionResponse>> cambio) {
        Preferences.Key<String> clave = claveDetalles();
        dataStore.updateDataAsync(preferencias -> {
            List<ApiClient.PublicacionResponse> detalles = leerLista(preferencias.get(clave));
            cambio.accept(detalles);
            MutablePreferences editables = preferencias.toMutablePreferences();
            editables.set(clave, gson.toJson(detalles));
            return Single.just(editables);
        }).subscribe(ok -> { }, error -> { });
    }

    /** Descarga todas las fotos al caché de disco de Glide para poder verlas sin conexión. */
    private void precargarFotos(ApiClient.PublicacionResponse publicacion) {
        List<String> fotos = publicacion.getFotos();
        if (fotos == null) {
            return;
        }
        hiloPrincipal.post(() -> {
            for (String foto : fotos) {
                Glide.with(appContext).load(ApiClient.imageUrl(foto)).preload();
            }
        });
    }

    private List<ApiClient.PublicacionResponse> leerLista(String json) {
        if (json == null) {
            return new ArrayList<>();
        }
        List<ApiClient.PublicacionResponse> lista = gson.fromJson(json, tipoLista);
        return lista != null ? new ArrayList<>(lista) : new ArrayList<>();
    }

    // Una copia por usuario: si en el dispositivo inicia sesión otra persona, no ve lo que vio la anterior.
    private Preferences.Key<String> claveHome() {
        return PreferencesKeys.stringKey("home_json_" + sesion.getUserId());
    }

    private Preferences.Key<Long> claveHomeFecha() {
        return PreferencesKeys.longKey("home_timestamp_" + sesion.getUserId());
    }

    private Preferences.Key<String> claveDetalles() {
        return PreferencesKeys.stringKey("detalles_json_" + sesion.getUserId());
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
