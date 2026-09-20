package com.ronda.backend.favorito;

import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import com.ronda.backend.publicacion.ExplorarFiltros;
import com.ronda.backend.publicacion.PublicacionService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BusquedaGuardadaService {
    private final BusquedaGuardadaRepository busquedas;
    private final UsuarioRepository usuarios;
    private final PublicacionService publicaciones;

    public BusquedaGuardadaService(BusquedaGuardadaRepository busquedas, UsuarioRepository usuarios,
                                   PublicacionService publicaciones) {
        this.busquedas = busquedas;
        this.usuarios = usuarios;
        this.publicaciones = publicaciones;
    }

    @Transactional
    public BusquedaGuardadaDtos.Item guardar(String email, BusquedaGuardadaDtos.CrearRequest request) {
        Usuario usuario = findUser(email);
        var filtros = new ExplorarFiltros(request.q(), request.categoria(), request.precioMin(),
                request.precioMax(), request.estadoArticulo(), request.lat(), request.lng(), request.radioKm());
        BusquedaGuardada guardada = busquedas.save(new BusquedaGuardada(usuario, request.nombre().trim(), filtros));
        return BusquedaGuardadaDtos.Item.from(guardada, 0);
    }

    @Transactional(readOnly = true)
    public List<BusquedaGuardadaDtos.Item> listar(String email) {
        Usuario usuario = findUser(email);
        return busquedas.findByUsuarioIdOrderByCreatedAtDesc(usuario.getId()).stream()
                .map(busqueda -> BusquedaGuardadaDtos.Item.from(busqueda,
                        publicaciones.contarNuevasDesde(busqueda.aFiltros(), busqueda.getUltimaRevisionAt())))
                .toList();
    }

    @Transactional
    public void marcarVista(String email, Long id) {
        buscarPropia(email, id).marcarVista();
    }

    @Transactional
    public void eliminar(String email, Long id) {
        busquedas.delete(buscarPropia(email, id));
    }

    private BusquedaGuardada buscarPropia(String email, Long id) {
        Usuario usuario = findUser(email);
        return busquedas.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Búsqueda guardada no encontrada"));
    }

    private Usuario findUser(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }
}
