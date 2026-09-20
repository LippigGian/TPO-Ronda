package com.example.ronda;

import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/** Páginas del ViewPager2 de fotos del detalle: una foto a pantalla completa por página. */
public class GaleriaAdapter extends RecyclerView.Adapter<GaleriaAdapter.ViewHolder> {

    private final List<String> fotos;

    public GaleriaAdapter(List<String> fotos) {
        this.fotos = fotos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imagen = new ImageView(parent.getContext());
        imagen.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imagen.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagen.setContentDescription(parent.getContext().getString(R.string.foto_publicacion));
        return new ViewHolder(imagen);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Glide.with(holder.imagen)
                .load(ApiClient.imageUrl(fotos.get(position)))
                .centerCrop()
                .into(holder.imagen);
    }

    @Override
    public int getItemCount() {
        return fotos.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imagen;

        ViewHolder(ImageView imagen) {
            super(imagen);
            this.imagen = imagen;
        }
    }
}
