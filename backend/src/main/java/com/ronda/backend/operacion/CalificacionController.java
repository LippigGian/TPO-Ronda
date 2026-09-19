package com.ronda.backend.operacion;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operaciones/{operacionId}/calificaciones")
public class CalificacionController {
    private final CalificacionService service;

    public CalificacionController(CalificacionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalificacionDtos.Response crear(@AuthenticationPrincipal UserDetails user, @PathVariable Long operacionId,
                                            @Valid @RequestBody CalificacionDtos.CreateRequest request) {
        return service.crear(user.getUsername(), operacionId, request);
    }
}
