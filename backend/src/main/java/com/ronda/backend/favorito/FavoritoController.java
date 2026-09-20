package com.ronda.backend.favorito;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FavoritoController {
    private final FavoritoService service;

    public FavoritoController(FavoritoService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/publicaciones/{id}/favorito")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcar(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        service.marcar(user.getUsername(), id);
    }

    @DeleteMapping("/api/v1/publicaciones/{id}/favorito")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desmarcar(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        service.desmarcar(user.getUsername(), id);
    }

    @PostMapping("/api/v1/favoritos/{publicacionId}/visto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarVisto(@AuthenticationPrincipal UserDetails user, @PathVariable Long publicacionId) {
        service.marcarVisto(user.getUsername(), publicacionId);
    }

    @GetMapping("/api/v1/favoritos")
    public List<FavoritoDtos.Item> listar(@AuthenticationPrincipal UserDetails user) {
        return service.listar(user.getUsername());
    }

    @GetMapping("/api/v1/favoritos/ids")
    public List<Long> ids(@AuthenticationPrincipal UserDetails user) {
        return service.idsFavoritos(user.getUsername());
    }
}
