package com.ronda.backend.publicacion;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicacionService {
    private final PublicacionRepository publicaciones;
    private final UsuarioRepository usuarios;
    private final FotoStorageService fotos;

    public PublicacionService(PublicacionRepository publicaciones, UsuarioRepository usuarios, FotoStorageService fotos) {
        this.publicaciones = publicaciones;
        this.usuarios = usuarios;
        this.fotos = fotos;
    }

    @Transactional
    public PublicacionDtos.Response create(String email, PublicacionDtos.CreateRequest request) {
        Usuario vendedor = findUser(email);
        return PublicacionDtos.Response.from(publicaciones.save(new Publicacion(vendedor, request)));
    }

    @Transactional(readOnly = true)
    public List<PublicacionDtos.Response> listMine(String email) {
        Usuario vendedor = findUser(email);
        return publicaciones.findByVendedorIdOrderByCreatedAtDesc(vendedor.getId()).stream()
                .map(PublicacionDtos.Response::from)
                .toList();
    }

    @Transactional
    public PublicacionDtos.Response changeStatus(String email, Long id, PublicacionDtos.ChangeStatusRequest request) {
        Usuario vendedor = findUser(email);
        Publicacion publicacion = publicaciones.findByIdAndVendedorId(id, vendedor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));
        publicacion.cambiarEstado(request.estado());
        return PublicacionDtos.Response.from(publicacion);
    }

    @Transactional
    public PublicacionDtos.Response addPhotos(String email, Long id, List<org.springframework.web.multipart.MultipartFile> archivos) {
        Usuario vendedor = findUser(email);
        Publicacion publicacion = publicaciones.findByIdAndVendedorId(id, vendedor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));
        if (archivos.isEmpty() || publicacion.getFotos().size() + archivos.size() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Podés cargar entre 1 y 5 fotos por publicación");
        }
        int orden = publicacion.getFotos().size();
        for (var archivo : archivos) {
            String nombre = fotos.save(archivo);
            publicacion.getFotos().add(new PublicacionFoto(publicacion, nombre, archivo.getContentType(), orden++));
        }
        return PublicacionDtos.Response.from(publicacion);
    }

    @Transactional
    public void delete(String email, Long id) {
        Usuario vendedor = findUser(email);
        Publicacion publicacion = publicaciones.findByIdAndVendedorId(id, vendedor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));
        publicacion.getFotos().forEach(foto -> fotos.delete(foto.getArchivo()));
        publicaciones.delete(publicacion);
    }

    private Usuario findUser(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }
}
