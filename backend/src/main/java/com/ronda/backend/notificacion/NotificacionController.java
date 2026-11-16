package com.ronda.backend.notificacion;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/** Endpoints del punto 11. Todos requieren JWT (SecurityConfig exige autenticacion por defecto). */
@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {
    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    /** Avisos que la app todavia no mostro. La app los consulta cada pocos segundos. */
    @GetMapping("/nuevas")
    public List<NotificacionDtos.Response> nuevas(@AuthenticationPrincipal UserDetails user) {
        return service.nuevas(user.getUsername());
    }

    /** Bandeja de avisos con la cantidad de no leidos. */
    @GetMapping
    public NotificacionDtos.BandejaResponse bandeja(@AuthenticationPrincipal UserDetails user) {
        return service.bandeja(user.getUsername());
    }

    @PostMapping("/{id}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLeida(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        service.marcarLeida(user.getUsername(), id);
    }

    @PostMapping("/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasLeidas(@AuthenticationPrincipal UserDetails user) {
        service.marcarTodasLeidas(user.getUsername());
    }
}
