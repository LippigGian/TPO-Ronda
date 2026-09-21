package com.ronda.backend.publicacion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FotoStorageService {
    private final Path uploadDir;

    public FotoStorageService(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo preparar el directorio de imágenes", exception);
        }
    }

    public String save(MultipartFile foto) {
        String contentType = foto.getContentType();
        if (foto.isEmpty() || contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada archivo debe ser una imagen");
        }

        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        String fileName = UUID.randomUUID() + extension;
        try (var input = foto.getInputStream()) {
            Files.copy(input, uploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen");
        }
    }

    public void delete(String fileName) {
        try {
            Files.deleteIfExists(uploadDir.resolve(fileName).normalize());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo eliminar la imagen");
        }
    }
}
