package com.ronda.backend.perfil;

import com.ronda.backend.auth.JwtService;
import com.ronda.backend.auth.Usuario;
import com.ronda.backend.auth.UsuarioRepository;
import com.ronda.backend.publicacion.EstadoPublicacion;
import com.ronda.backend.publicacion.FotoStorageService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PerfilService {
    /** Se muestra en el perfil publico si el usuario todavia no cargo su nombre. */
    private static final String NOMBRE_POR_DEFECTO = "Usuario de Ronda";

    private final UsuarioRepository usuarios;
    private final PerfilRepository perfiles;
    private final PerfilPublicacionRepository publicaciones;
    private final ReputacionPerfilService reputacionService;
    private final FotoStorageService fotos;
    private final JwtService jwtService;

    public PerfilService(UsuarioRepository usuarios, PerfilRepository perfiles,
                         PerfilPublicacionRepository publicaciones, ReputacionPerfilService reputacionService,
                         FotoStorageService fotos, JwtService jwtService) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.publicaciones = publicaciones;
        this.reputacionService = reputacionService;
        this.fotos = fotos;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public PerfilDtos.MiPerfilResponse obtenerMiPerfil(String email) {
        Usuario usuario = buscarPorEmail(email);
        return armarMiPerfil(usuario, obtenerOCrear(usuario));
    }

    @Transactional
    public PerfilDtos.ActualizarPerfilResponse actualizarMiPerfil(String email, PerfilDtos.ActualizarPerfilRequest request) {
        Usuario usuario = buscarPorEmail(email);
        Perfil perfil = obtenerOCrear(usuario);

        String tokenNuevo = actualizarEmailSiCambio(usuario, request.email());

        perfil.actualizarDatos(limpiar(request.nombre()), limpiar(request.telefono()), limpiar(request.zona()));
        perfiles.save(perfil);
        return new PerfilDtos.ActualizarPerfilResponse(armarMiPerfil(usuario, perfil), tokenNuevo);
    }

    /**
     * Actualiza el email de la cuenta si vino distinto al actual, validando que no
     * este en uso por otro usuario. Devuelve un JWT nuevo (subject = email nuevo)
     * cuando hubo cambio, o null si el email se mantuvo igual.
     */
    private String actualizarEmailSiCambio(Usuario usuario, String emailSolicitado) {
        String nuevoEmail = emailSolicitado.trim().toLowerCase();
        if (nuevoEmail.equalsIgnoreCase(usuario.getEmail())) {
            return null;
        }
        if (usuarios.existsByEmailIgnoreCase(nuevoEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está en uso");
        }
        usuario.cambiarEmail(nuevoEmail);
        usuarios.save(usuario);
        return jwtService.createToken(usuario);
    }

    @Transactional
    public PerfilDtos.MiPerfilResponse actualizarFoto(String email, MultipartFile foto) {
        Usuario usuario = buscarPorEmail(email);
        Perfil perfil = obtenerOCrear(usuario);
        String nuevoArchivo = fotos.save(foto); // valida que sea una imagen
        String archivoAnterior = perfil.cambiarFoto(nuevoArchivo);
        perfiles.save(perfil);
        if (archivoAnterior != null) {
            fotos.delete(archivoAnterior);
        }
        return armarMiPerfil(usuario, perfil);
    }

    @Transactional(readOnly = true)
    public PerfilDtos.PerfilPublicoResponse obtenerPerfilPublico(Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Perfil perfil = obtenerOCrear(usuario);

        List<PerfilDtos.PublicacionResumen> activas = publicaciones
                .findByVendedorIdAndEstadoPublicacionOrderByCreatedAtDesc(usuarioId, EstadoPublicacion.ACTIVA)
                .stream()
                .map(PerfilDtos.PublicacionResumen::from)
                .toList();

        return new PerfilDtos.PerfilPublicoResponse(usuario.getId(), nombrePublico(usuario, perfil), perfil.getZona(),
                perfil.getFotoUrl(), usuario.getCreatedAt(), reputacionService.calcular(usuarioId), activas);
    }

    /**
     * Nombre que ven los demas usuarios. Lo usan el perfil publico y el detalle de publicacion,
     * asi el vendedor se muestra igual en toda la app.
     */
    @Transactional(readOnly = true)
    public String nombrePublico(Usuario usuario) {
        return nombrePublico(usuario, obtenerOCrear(usuario));
    }

    // ---------- helpers privados ----------

    /**
     * Prioridad: nombre cargado en el perfil; si no hay, nombre_usuario de la cuenta,
     * salvo que sea un email (el registro lo inicializa con el email y no debe exponerse).
     */
    private static String nombrePublico(Usuario usuario, Perfil perfil) {
        if (perfil.getNombre() != null) {
            return perfil.getNombre();
        }
        String nombreCuenta = usuario.getNombreUsuario();
        if (nombreCuenta != null && !nombreCuenta.isBlank() && !nombreCuenta.contains("@")) {
            return nombreCuenta;
        }
        return NOMBRE_POR_DEFECTO;
    }

    private PerfilDtos.MiPerfilResponse armarMiPerfil(Usuario usuario, Perfil perfil) {
        return new PerfilDtos.MiPerfilResponse(usuario.getId(), usuario.getEmail(), perfil.getNombre(),
                perfil.getTelefono(), perfil.getZona(), perfil.getFotoUrl(), usuario.getCreatedAt(),
                reputacionService.calcular(usuario.getId()));
    }

    /** Si el usuario nunca edito su perfil, devuelve uno vacio (se persiste recien al guardar). */
    private Perfil obtenerOCrear(Usuario usuario) {
        return perfiles.findById(usuario.getId()).orElseGet(() -> new Perfil(usuario.getId()));
    }

    private Usuario buscarPorEmail(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }

    /** Recorta espacios y convierte textos vacios en null para no guardar "" en la base. */
    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
