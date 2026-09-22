package com.ronda.backend.perfil;

import com.ronda.backend.publicacion.EstadoArticulo;
import com.ronda.backend.publicacion.Publicacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contratos JSON de la feature de perfil. Mismo estilo que AuthDtos y PublicacionDtos. */
public final class PerfilDtos {
    private PerfilDtos() { }

    public record ActualizarPerfilRequest(
            // Mismas reglas que AuthDtos.RegisterRequest, ya que el email tambien identifica la cuenta.
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 120) String nombre,
            // Opcional: vacio o entre 6 y 30 caracteres de digitos, espacios, +, -, ( y ).
            @Size(max = 30) @Pattern(regexp = "^$|^[0-9+()\\s-]{6,30}$") String telefono,
            @Size(max = 120) String zona) { }

    /**
     * Respuesta de actualizarMiPerfil. token viene con valor solo cuando el email cambio:
     * el JWT anterior queda invalido (su subject es el email viejo), asi el front
     * puede guardar la sesion nueva sin pedir relogin.
     */
    public record ActualizarPerfilResponse(MiPerfilResponse perfil, String token) { }

    /**
     * Reputacion calculada, nunca almacenada.
     * promedioEstrellas es null mientras el usuario no tenga calificaciones.
     */
    public record Reputacion(Double promedioEstrellas,
                             long cantidadCalificaciones,
                             long ventasConcretadas,
                             long comprasConcretadas) { }

    /** Perfil propio: incluye datos privados (email y telefono). */
    public record MiPerfilResponse(Long id, String email, String nombre, String telefono, String zona,
                                   String foto, Instant miembroDesde, Reputacion reputacion) { }

    /** Perfil publico: lo ve cualquier usuario, por eso NO incluye email ni telefono. */
    public record PerfilPublicoResponse(Long id, String nombre, String zona, String foto,
                                        Instant miembroDesde, Reputacion reputacion,
                                        List<PublicacionResumen> publicacionesActivas) { }

    /** Version liviana de una publicacion para listarla dentro del perfil publico. */
    public record PublicacionResumen(Long id, String titulo, BigDecimal precio,
                                     EstadoArticulo estadoArticulo, String foto) {
        static PublicacionResumen from(Publicacion publicacion) {
            String foto = publicacion.getFotos().isEmpty()
                    ? null
                    : "/uploads/" + publicacion.getFotos().get(0).getArchivo();
            return new PublicacionResumen(publicacion.getId(), publicacion.getTitulo(),
                    publicacion.getPrecio(), publicacion.getEstadoArticulo(), foto);
        }
    }
}
