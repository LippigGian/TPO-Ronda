package com.example.ronda.notificacion;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.ronda.DetallePublicacionActivity;
import com.example.ronda.HomeActivity;
import com.example.ronda.R;
import com.example.ronda.oferta.MisOfertasActivity;

/**
 * Punto 11: muestra cada aviso como notificación del sistema.
 * Al tocarla se abre directamente la oferta (en "Mis ofertas") o la publicación correspondiente.
 */
public final class AvisosNotifier {

    public static final String CANAL = "avisos_ronda";
    private static final int PEDIDO_PERMISO = 1101;

    private AvisosNotifier() { }

    /** Canal de Android 8+: lo crea una sola vez el sistema aunque se llame varias veces. */
    public static void crearCanal(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel canal = new NotificationChannel(CANAL, "Preguntas y ofertas",
                NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription("Avisos de preguntas, respuestas y ofertas de tus publicaciones");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(canal);
        }
    }

    /** Android 13+ pide permiso para mostrar notificaciones. Se pide al entrar al Home. */
    public static void pedirPermisoSiHaceFalta(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !tienePermiso(activity)) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, PEDIDO_PERMISO);
        }
    }

    public static boolean tienePermiso(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void mostrar(Context context, NotificacionModels.Aviso aviso) {
        if (!tienePermiso(context)) {
            return;
        }
        int idNotificacion = (int) aviso.getId();
        PendingIntent alTocar = PendingIntent.getActivity(context, idNotificacion, destino(context, aviso),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificacion = new NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setColor(ContextCompat.getColor(context, R.color.ronda_primary))
                .setContentTitle(aviso.getTitulo())
                .setContentText(aviso.getMensaje())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(aviso.getMensaje()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(alTocar);
        try {
            NotificationManagerCompat.from(context).notify(idNotificacion, notificacion.build());
        } catch (SecurityException sinPermiso) {
            // El permiso se revocó entre el chequeo y la notificación: el aviso igual queda en el backend.
        }
    }

    /** Oferta si el aviso tiene una; si no, la publicación; como último recurso, el Home. */
    static Intent destino(Context context, NotificacionModels.Aviso aviso) {
        Intent intent;
        if (aviso.getOfertaId() != null) {
            intent = MisOfertasActivity.crearIntent(context, aviso.getOfertaId());
        } else if (aviso.getPublicacionId() != null) {
            intent = DetallePublicacionActivity.crearIntent(context, aviso.getPublicacionId());
        } else {
            intent = new Intent(context, HomeActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }
}
