package com.example.ronda.pregunta;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.ronda.ApiClient;
import com.example.ronda.ConnectivityObserver;
import com.example.ronda.Formato;
import com.example.ronda.R;
import com.example.ronda.util.ApiCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Punto 4: sección "Preguntas" del detalle de una publicación.
 * El interesado pregunta; el vendedor ve las preguntas y responde cada una una sola vez.
 * Punto 11: el backend avisa al vendedor de cada pregunta y a quien preguntó de cada respuesta.
 */
public final class PreguntasSeccion {

    private static final int MIN_PREGUNTA = 3;
    private static final int MAX_TEXTO = 500;

    private final Activity activity;
    private final LinearLayout contenedor;
    private final TextView tvEstado;
    private final PreguntaApi api = ApiClient.crearServicio(PreguntaApi.class);
    private long publicacionId = -1;

    public PreguntasSeccion(Activity activity) {
        this.activity = activity;
        this.contenedor = activity.findViewById(R.id.contenedorPreguntas);
        this.tvEstado = activity.findViewById(R.id.tvEstadoPreguntas);
    }

    /** Carga las preguntas de la publicación. Sin conexión deja un aviso y no borra lo que ya se ve. */
    public void cargar(long publicacionId) {
        this.publicacionId = publicacionId;
        if (!ConnectivityObserver.isOnline(activity)) {
            if (contenedor.getChildCount() == 0) {
                mostrarEstado(activity.getString(R.string.preguntas_sin_conexion));
            }
            return;
        }
        api.listar(publicacionId).enqueue(new Callback<List<PreguntaModels.Pregunta>>() {
            @Override
            public void onResponse(Call<List<PreguntaModels.Pregunta>> call,
                                   Response<List<PreguntaModels.Pregunta>> response) {
                if (activity.isFinishing()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    mostrar(response.body());
                } else if (contenedor.getChildCount() == 0) {
                    mostrarEstado(activity.getString(R.string.preguntas_error));
                }
            }

            @Override
            public void onFailure(Call<List<PreguntaModels.Pregunta>> call, Throwable error) {
                if (!activity.isFinishing() && contenedor.getChildCount() == 0) {
                    mostrarEstado(activity.getString(R.string.preguntas_sin_conexion));
                }
            }
        });
    }

    /** Botón "Preguntar" del detalle. */
    public void preguntar() {
        if (!hayConexion() || publicacionId == -1) {
            return;
        }
        pedirTexto(activity.getString(R.string.preguntas_dialogo_preguntar),
                activity.getString(R.string.preguntas_hint_pregunta),
                activity.getString(R.string.preguntas_enviar), MIN_PREGUNTA, texto ->
                        api.preguntar(publicacionId, new PreguntaModels.PreguntarRequest(texto))
                                .enqueue(new ApiCallback<PreguntaModels.Pregunta>(activity,
                                        "No se pudo enviar la pregunta") {
                                    @Override
                                    protected void onExito(PreguntaModels.Pregunta pregunta) {
                                        Toast.makeText(activity, R.string.preguntas_enviada, Toast.LENGTH_LONG).show();
                                        cargar(publicacionId);
                                    }
                                }));
    }

    private void responder(PreguntaModels.Pregunta pregunta) {
        if (!hayConexion()) {
            return;
        }
        pedirTexto(activity.getString(R.string.preguntas_dialogo_responder),
                "“" + pregunta.getTexto() + "”", activity.getString(R.string.preguntas_responder), 1, texto ->
                        api.responder(pregunta.getId(), new PreguntaModels.ResponderRequest(texto))
                                .enqueue(new ApiCallback<PreguntaModels.Pregunta>(activity,
                                        "No se pudo enviar la respuesta") {
                                    @Override
                                    protected void onExito(PreguntaModels.Pregunta respondida) {
                                        Toast.makeText(activity, R.string.preguntas_respondida, Toast.LENGTH_SHORT).show();
                                        cargar(publicacionId);
                                    }
                                }));
    }

    private void mostrar(List<PreguntaModels.Pregunta> preguntas) {
        contenedor.removeAllViews();
        if (preguntas.isEmpty()) {
            mostrarEstado(activity.getString(R.string.preguntas_vacio));
            return;
        }
        tvEstado.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (PreguntaModels.Pregunta pregunta : preguntas) {
            View fila = inflater.inflate(R.layout.item_pregunta, contenedor, false);
            ((TextView) fila.findViewById(R.id.tvTextoPregunta)).setText(pregunta.getTexto());
            String autor = pregunta.isEsMia() ? activity.getString(R.string.preguntas_vos) : pregunta.getAutor();
            ((TextView) fila.findViewById(R.id.tvMetaPregunta))
                    .setText(autor + " · " + Formato.fecha(pregunta.getCreatedAt()));

            TextView tvRespuesta = fila.findViewById(R.id.tvRespuestaPregunta);
            View btnResponder = fila.findViewById(R.id.btnResponderPregunta);
            if (pregunta.getRespuesta() != null) {
                tvRespuesta.setText(activity.getString(R.string.preguntas_respuesta_prefijo, pregunta.getRespuesta()));
                tvRespuesta.setVisibility(View.VISIBLE);
            } else {
                tvRespuesta.setText(R.string.preguntas_sin_respuesta);
                tvRespuesta.setVisibility(pregunta.isPuedoResponder() ? View.GONE : View.VISIBLE);
            }
            btnResponder.setVisibility(pregunta.isPuedoResponder() ? View.VISIBLE : View.GONE);
            btnResponder.setOnClickListener(v -> responder(pregunta));
            contenedor.addView(fila);
        }
    }

    private void mostrarEstado(String texto) {
        tvEstado.setText(texto);
        tvEstado.setVisibility(View.VISIBLE);
    }

    private boolean hayConexion() {
        if (ConnectivityObserver.isOnline(activity)) {
            return true;
        }
        Toast.makeText(activity, R.string.detalle_requiere_conexion, Toast.LENGTH_LONG).show();
        return false;
    }

    private interface AlConfirmar {
        void onTexto(String texto);
    }

    /** Diálogo con un campo de texto; no se cierra si el texto no es válido. */
    private void pedirTexto(String titulo, String ayuda, String textoConfirmar, int minimo, AlConfirmar alConfirmar) {
        View vista = LayoutInflater.from(activity).inflate(R.layout.dialog_pregunta, null);
        TextView tvAyuda = vista.findViewById(R.id.tvAyudaPregunta);
        TextInputLayout til = vista.findViewById(R.id.tilTextoPregunta);
        TextInputEditText et = vista.findViewById(R.id.etTextoPregunta);
        tvAyuda.setText(ayuda);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(titulo)
                .setView(vista)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(textoConfirmar, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String texto = et.getText() != null ? et.getText().toString().trim() : "";
            if (texto.length() < minimo) {
                til.setError(minimo > 1 ? "Escribí al menos " + minimo + " caracteres" : "Escribí la respuesta");
                return;
            }
            if (texto.length() > MAX_TEXTO) {
                til.setError("Máximo " + MAX_TEXTO + " caracteres");
                return;
            }
            alConfirmar.onTexto(texto);
            dialog.dismiss();
        }));
        dialog.show();
    }
}
