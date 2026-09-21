package com.ronda.backend.perfil;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints del punto 2 (Perfil y Reputacion). Todos requieren JWT:
 * SecurityConfig ya exige autenticacion para cualquier ruta no listada como publica.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public class PerfilController {
    private final PerfilService service;

    public PerfilController(PerfilService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public PerfilDtos.MiPerfilResponse miPerfil(@AuthenticationPrincipal UserDetails user) {
        return service.obtenerMiPerfil(user.getUsername());
    }

    @PutMapping("/me")
    public PerfilDtos.MiPerfilResponse actualizarMiPerfil(@AuthenticationPrincipal UserDetails user,
                                                          @Valid @RequestBody PerfilDtos.ActualizarPerfilRequest request) {
        return service.actualizarMiPerfil(user.getUsername(), request);
    }

    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PerfilDtos.MiPerfilResponse actualizarFoto(@AuthenticationPrincipal UserDetails user,
                                                      @RequestPart("foto") MultipartFile foto) {
        return service.actualizarFoto(user.getUsername(), foto);
    }

    @GetMapping("/{id}/perfil-publico")
    public PerfilDtos.PerfilPublicoResponse perfilPublico(@PathVariable Long id) {
        return service.obtenerPerfilPublico(id);
    }
}
