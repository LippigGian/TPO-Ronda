package com.example.ronda.util;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/** Convierte una imagen elegida de la galeria (Uri) en una parte multipart para Retrofit. */
public final class MultipartHelper {
    private MultipartHelper() { }

    /**
     * @param campo nombre del campo que espera el backend (ej: "foto" o "fotos")
     */
    public static MultipartBody.Part imagenDesdeUri(Context context, Uri uri, String campo,
                                                    String nombreArchivo) throws IOException {
        String contentType = context.getContentResolver().getType(uri);
        if (contentType == null) {
            contentType = "image/jpeg";
        }

        byte[] bytes = leerBytes(context, uri);
        RequestBody body = RequestBody.create(MediaType.parse(contentType), bytes);
        return MultipartBody.Part.createFormData(campo, nombreArchivo, body);
    }

    private static byte[] leerBytes(Context context, Uri uri) throws IOException {
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) {
                throw new IOException("No se pudo leer la imagen");
            }
            byte[] buffer = new byte[8192];
            int leidos;
            while ((leidos = input.read(buffer)) != -1) {
                output.write(buffer, 0, leidos);
            }
            return output.toByteArray();
        }
    }
}
