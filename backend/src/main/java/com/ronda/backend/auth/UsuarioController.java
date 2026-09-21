package com.ronda.backend.auth;

import com.ronda.backend.operacion.ReputacionDtos;
import com.ronda.backend.operacion.ReputacionService;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UsuarioRepository usuarios;
    private final ReputacionService reputacionService;

    public UsuarioController(UsuarioRepository usuarios, ReputacionService reputacionService) {
        this.usuarios = usuarios;
        this.reputacionService = reputacionService;
    }

    @GetMapping("/buscar")
    public UsuarioDtos.PublicResponse buscarPorEmail(@RequestParam String email) {
        if (!EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email inválido");
        }
        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return UsuarioDtos.PublicResponse.from(usuario);
    }

    @GetMapping("/{id}/reputacion")
    public ReputacionDtos.Response reputacion(@PathVariable Long id) {
        return reputacionService.obtener(id);
    }
}
