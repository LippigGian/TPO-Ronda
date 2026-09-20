package com.ronda.backend.oferta;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/** Endpoints del punto 7. Todos requieren JWT (SecurityConfig exige autenticacion por defecto). */
@RestController
@RequestMapping("/api/v1")
public class OfertaController {
    private final OfertaService service;

    public OfertaController(OfertaService service) {
        this.service = service;
    }

    @PostMapping("/publicaciones/{publicacionId}/ofertas")
    @ResponseStatus(HttpStatus.CREATED)
    public OfertaDtos.Response ofertar(@AuthenticationPrincipal UserDetails user, @PathVariable Long publicacionId,
                                       @Valid @RequestBody OfertaDtos.PropuestaRequest request) {
        return service.ofertar(user.getUsername(), publicacionId, request);
    }

    @GetMapping("/ofertas/mias")
    public OfertaDtos.MisOfertasResponse misOfertas(@AuthenticationPrincipal UserDetails user) {
        return service.misOfertas(user.getUsername());
    }

    @GetMapping("/ofertas/{id}")
    public OfertaDtos.Response obtener(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return service.obtener(user.getUsername(), id);
    }

    @PostMapping("/ofertas/{id}/aceptar")
    public OfertaDtos.Response aceptar(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return service.aceptar(user.getUsername(), id);
    }

    @PostMapping("/ofertas/{id}/rechazar")
    public OfertaDtos.Response rechazar(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return service.rechazar(user.getUsername(), id);
    }

    @PostMapping("/ofertas/{id}/contraofertar")
    public OfertaDtos.Response contraofertar(@AuthenticationPrincipal UserDetails user, @PathVariable Long id,
                                             @Valid @RequestBody OfertaDtos.PropuestaRequest request) {
        return service.contraofertar(user.getUsername(), id, request);
    }
}
