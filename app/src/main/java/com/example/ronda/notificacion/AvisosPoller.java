package com.example.ronda.notificacion;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.ronda.ApiClient;
import com.example.ronda.ConnectivityObserver;
import com.example.ronda.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Punto 11 (sin Firebase): mientras la app está abierta o en segundo plano, consulta al backend
 * cada pocos segundos si hay avisos nuevos y los muestra como notificación del sistema.
 * Sin sesión o sin conexión no consulta. El backend marca cada aviso como entregado, así que
 * nunca se muestra dos veces.
 */
public final class AvisosPoller {

    private static final long INTERVALO_MS = 8_000L;
    private static AvisosPoller instancia;

    private final Context context;
    private final SessionManager sesion;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private NotificacionApi api;
    private boolean consultando;

    private final Runnable ciclo = new Runnable() {
        @Override
        public void run() {
            consultar();
            handler.postDelayed(this, INTERVALO_MS);
        }
    };

    private AvisosPoller(Context context) {
        this.context = context.getApplicationContext();
        this.sesion = new SessionManager(this.context);
    }

    /** Se llama una vez desde RondaApplication, después de inicializar ApiClient. */
    public static synchronized void iniciar(Context context) {
        if (instancia != null) {
            return;
        }
        AvisosNotifier.crearCanal(context);
        instancia = new AvisosPoller(context);
        instancia.handler.post(instancia.ciclo);
    }

    /** Consulta ya, sin esperar el próximo ciclo (por ejemplo, después de iniciar sesión). */
    public static void consultarAhora() {
        if (instancia != null) {
            instancia.handler.post(instancia::consultar);
        }
    }

    private void consultar() {
        if (consultando || sesion.getToken() == null || !ConnectivityObserver.isOnline(context)) {
            return;
        }
        if (api == null) {
            api = ApiClient.crearServicio(NotificacionApi.class);
        }
        consultando = true;
        api.nuevas().enqueue(new Callback<List<NotificacionModels.Aviso>>() {
            @Override
            public void onResponse(Call<List<NotificacionModels.Aviso>> call,
                                   Response<List<NotificacionModels.Aviso>> response) {
                consultando = false;
                if (response.isSuccessful() && response.body() != null) {
                    for (NotificacionModels.Aviso aviso : response.body()) {
                        AvisosNotifier.mostrar(context, aviso);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<NotificacionModels.Aviso>> call, Throwable t) {
                // Backend caído o red inestable: se reintenta en el próximo ciclo, sin molestar al usuario.
                consultando = false;
            }
        });
    }
}
