package com.example.ronda;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tarjetas del Home. Soporta agregar páginas nuevas al final (scroll infinito). */
public class PublicacionesAdapter extends RecyclerView.Adapter<PublicacionesAdapter.ViewHolder> {

    public interface OnPublicacionClick {
        void onClick(ApiClient.PublicacionResponse publicacion);
    }

    /** Punto 10: tocar el corazón de una card. esFavoritoActual es el estado antes de tocar. */
    public interface OnFavoritoClick {
        void onClick(ApiClient.PublicacionResponse publicacion, boolean esFavoritoActual);
    }

    private final List<ApiClient.PublicacionResponse> items = new ArrayList<>();
    private final Set<Long> favoritoIds = new HashSet<>();
    private final OnPublicacionClick onClick;
    private final OnFavoritoClick onFavoritoClick;

    public PublicacionesAdapter(OnPublicacionClick onClick, OnFavoritoClick onFavoritoClick) {
        this.onClick = onClick;
        this.onFavoritoClick = onFavoritoClick;
    }

    public void reemplazar(List<ApiClient.PublicacionResponse> nuevos) {
        items.clear();
        items.addAll(nuevos);
        notifyDataSetChanged();
    }

    public void agregar(List<ApiClient.PublicacionResponse> nuevos) {
        int inicio = items.size();
        items.addAll(nuevos);
        notifyItemRangeInserted(inicio, nuevos.size());
    }

    /** Se llama una vez al cargar el Home con los favoritos actuales del usuario. */
    public void setFavoritoIds(Set<Long> ids) {
        favoritoIds.clear();
        favoritoIds.addAll(ids);
        notifyDataSetChanged();
    }

    /** Actualiza el corazón de una card puntual después de marcar/desmarcar favorito. */
    public void marcarFavorito(long publicacionId, boolean favorito) {
        if (favorito) {
            favoritoIds.add(publicacionId);
        } else {
            favoritoIds.remove(publicacionId);
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == publicacionId) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_publicacion, parent, false);
        return new ViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApiClient.PublicacionResponse publicacion = items.get(position);

        holder.titulo.setText(publicacion.getTitulo());
        holder.precio.setText(Formato.precio(publicacion.getPrecio()));
        holder.estado.setText(Formato.estadoArticulo(publicacion.getEstadoArticulo()));
        holder.zona.setText(textoUbicacion(publicacion));

        boolean esFavorito = favoritoIds.contains(publicacion.getId());
        holder.btnFavorito.setImageResource(esFavorito ? R.drawable.ic_favorito_lleno : R.drawable.ic_favorito_borde);
        holder.btnFavorito.setOnClickListener(v -> onFavoritoClick.onClick(publicacion, esFavorito));

        if (publicacion.getFotos() != null && !publicacion.getFotos().isEmpty()) {
            Glide.with(holder.foto)
                    .load(ApiClient.imageUrl(publicacion.getFotos().get(0)))
                    .centerCrop()
                    .into(holder.foto);
        } else {
            Glide.with(holder.foto).clear(holder.foto);
            holder.foto.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> onClick.onClick(publicacion));
    }

    private static String textoUbicacion(ApiClient.PublicacionResponse publicacion) {
        String zona = publicacion.getZona() != null ? publicacion.getZona() : "Zona no informada";
        if (publicacion.getDistanciaKm() != null) {
            return zona + " · a ~" + publicacion.getDistanciaKm() + " km";
        }
        return zona;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView foto;
        final ImageButton btnFavorito;
        final TextView titulo;
        final TextView precio;
        final TextView estado;
        final TextView zona;

        ViewHolder(View itemView) {
            super(itemView);
            foto = itemView.findViewById(R.id.ivFotoPublicacion);
            btnFavorito = itemView.findViewById(R.id.btnFavoritoPublicacion);
            titulo = itemView.findViewById(R.id.tvTituloPublicacion);
            precio = itemView.findViewById(R.id.tvPrecioPublicacion);
            estado = itemView.findViewById(R.id.tvEstadoArticulo);
            zona = itemView.findViewById(R.id.tvZonaPublicacion);
        }
    }
}
