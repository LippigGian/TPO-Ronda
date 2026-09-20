package com.ronda.backend.publicacion;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicacionService {
    private static final int MAX_PAGE_SIZE = 50;

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
    public PublicacionDtos.PageResponse<PublicacionDtos.Resumen> explorar(
            ExplorarFiltros filtros, OrdenPublicaciones orden, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), orden.sort());
        var resultado = publicaciones.findAll(PublicacionSpecs.explorar(filtros), pageable)
                .map(publicacion -> PublicacionDtos.Resumen.from(publicacion, distanciaKm(filtros, publicacion)));
        return PublicacionDtos.PageResponse.from(resultado);
    }

    @Transactional(readOnly = true)
    public PublicacionDtos.Detalle detalle(String email, Long id) {
        Usuario usuario = findUser(email);
        Publicacion publicacion = publicaciones.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));
        boolean esPropia = publicacion.getVendedor().getId().equals(usuario.getId());
        // Pausadas y vendidas solo las ve su dueño; para el resto es como si no existieran.
        if (!esPropia && publicacion.getEstadoPublicacion() != EstadoPublicacion.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada");
        }
        // TODO punto 7 (ofertas): también debe ser visible si el usuario tiene una oferta ACEPTADA sobre esta publicación.
        boolean direccionVisible = esPropia;
        // TODO punto 2/9: reemplazar por la reputación real cuando existan las calificaciones.
        return PublicacionDtos.Detalle.from(publicacion, esPropia, direccionVisible, PublicacionDtos.Reputacion.sinDatos());
    }

    @Transactional(readOnly = true)
    public List<String> categorias() {
        return publicaciones.findCategoriasActivas();
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

    /** Distancia aproximada (km enteros, mínimo 1) para no permitir ubicar la dirección exacta. */
    private static Integer distanciaKm(ExplorarFiltros filtros, Publicacion publicacion) {
        if (filtros.lat() == null || filtros.lng() == null) {
            return null;
        }
        double lat1 = Math.toRadians(filtros.lat());
        double lat2 = Math.toRadians(publicacion.getLatitud().doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(publicacion.getLongitud().doubleValue() - filtros.lng());
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLng / 2), 2);
        double km = 2 * 6371.0 * Math.asin(Math.min(1.0, Math.sqrt(a)));
        return (int) Math.max(1, Math.round(km));
    }

    private Usuario findUser(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }
}
