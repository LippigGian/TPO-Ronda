package com.example.ronda.perfil;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.example.ronda.ApiClient;
import com.example.ronda.R;
import com.example.ronda.util.ApiCallback;

import java.util.List;

/**
 * Punto 2: perfil publico de cualquier usuario (reputacion, antiguedad y publicaciones activas).
 *
 * Para abrirlo desde otra pantalla (por ejemplo, el detalle de una publicacion):
 *   PerfilPublicoActivity.abrir(context, vendedorId);
 */
public class PerfilPublicoActivity extends AppCompatActivity {

    private static final String EXTRA_USUARIO_ID = "extra_usuario_id";

    private TextView tvNombre;
    private TextView tvZona;
    private TextView tvMiembroDesde;
    private TextView tvEstrellas;
    private TextView tvOperaciones;
    private TextView tvSinPublicaciones;
    private ImageView ivFoto;
    private LinearLayout contenedorPublicaciones;

    /** Punto de entrada unico: asi nadie tiene que conocer el nombre del extra. */
    public static void abrir(Context context, long usuarioId) {
        Intent intent = new Intent(context, PerfilPublicoActivity.class);
        intent.putExtra(EXTRA_USUARIO_ID, usuarioId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil_publico);
        aplicarInsets();
        vincularVistas();

        long usuarioId = getIntent().getLongExtra(EXTRA_USUARIO_ID, -1);
        if (usuarioId <= 0) {
            Toast.makeText(this, "Usuario inválido", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        cargarPerfil(usuarioId);
    }

    private void vincularVistas() {
        ivFoto = findViewById(R.id.ivFotoPerfilPublico);
        tvNombre = findViewById(R.id.tvNombrePublico);
        tvZona = findViewById(R.id.tvZonaPublica);
        tvMiembroDesde = findViewById(R.id.tvMiembroDesdePublico);
        tvEstrellas = findViewById(R.id.tvEstrellasPublico);
        tvOperaciones = findViewById(R.id.tvOperacionesPublico);
        tvSinPublicaciones = findViewById(R.id.tvSinPublicacionesActivas);
        contenedorPublicaciones = findViewById(R.id.contenedorPublicacionesActivas);
    }

    private void cargarPerfil(long usuarioId) {
        ApiClient.crearServicio(PerfilApi.class)
                .obtenerPerfilPublico(usuarioId)
                .enqueue(new ApiCallback<PerfilModels.PerfilPublico>(this, "No se pudo cargar el perfil") {
                    @Override
                    protected void onExito(PerfilModels.PerfilPublico perfil) {
                        mostrarPerfil(perfil);
                    }
                });
    }

    private void mostrarPerfil(PerfilModels.PerfilPublico perfil) {
        tvNombre.setText(perfil.getNombre());
        tvZona.setText(perfil.getZona() == null ? "Zona no informada" : perfil.getZona());
        tvMiembroDesde.setText(PerfilFormatter.miembroDesde(perfil.getMiembroDesde()));
        tvEstrellas.setText(PerfilFormatter.estrellas(perfil.getReputacion()));
        tvOperaciones.setText(PerfilFormatter.operaciones(perfil.getReputacion()));
        cargarImagen(perfil.getFoto(), ivFoto, R.drawable.ic_perfil_placeholder);
        mostrarPublicaciones(perfil.getPublicacionesActivas());
    }

    private void mostrarPublicaciones(List<PerfilModels.PublicacionResumen> publicaciones) {
        contenedorPublicaciones.removeAllViews();

        boolean vacia = publicaciones == null || publicaciones.isEmpty();
        tvSinPublicaciones.setVisibility(vacia ? View.VISIBLE : View.GONE);
        if (vacia) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (PerfilModels.PublicacionResumen publicacion : publicaciones) {
            View fila = inflater.inflate(R.layout.item_publicacion_resumen, contenedorPublicaciones, false);

            ((TextView) fila.findViewById(R.id.tvTituloResumen)).setText(publicacion.getTitulo());
            ((TextView) fila.findViewById(R.id.tvPrecioResumen))
                    .setText(PerfilFormatter.precio(publicacion.getPrecio()));
            ((TextView) fila.findViewById(R.id.tvEstadoResumen))
                    .setText(PerfilFormatter.estadoArticulo(publicacion.getEstadoArticulo()));
            cargarImagen(publicacion.getFoto(), fila.findViewById(R.id.ivFotoResumen), 0);

            // TODO: cuando exista el detalle de publicacion (punto 4), abrirlo al tocar la fila.

            contenedorPublicaciones.addView(fila);
        }
    }

    /** @param placeholder recurso a mostrar si no hay foto, o 0 para dejar el fondo vacio. */
    private void cargarImagen(String rutaRelativa, ImageView destino, int placeholder) {
        String url = rutaRelativa == null ? null : ApiClient.imageUrl(rutaRelativa);
        RequestBuilder<Drawable> request = Glide.with(this).load(url).centerCrop();
        if (placeholder != 0) {
            request = request.placeholder(placeholder).fallback(placeholder).error(placeholder);
        }
        request.into(destino);
    }

    private void aplicarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
