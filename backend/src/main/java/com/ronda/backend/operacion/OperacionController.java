package com.ronda.backend.operacion;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
public class OperacionController {
    private final OperacionService service;

    public OperacionController(OperacionService service) {
        this.service = service;
    }

    /** Cierre con QR: el comprador envia el id de publicacion leido del QR fisico de la entrega. */
    @PostMapping("/api/v1/operaciones/confirmar-recepcion")
    @ResponseStatus(HttpStatus.CREATED)
    public OperacionDtos.HistorialItemResponse confirmarRecepcion(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody OperacionDtos.ConfirmarRecepcionRequest request) {
        return service.confirmarRecepcion(user.getUsername(), request);
    }

    @GetMapping("/api/v1/operaciones/historial")
    public List<OperacionDtos.HistorialItemResponse> historial(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "TODAS") TipoOperacion tipo,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta) {
        return service.historial(user.getUsername(), tipo, desde, hasta);
    }

    @GetMapping("/api/v1/operaciones/pendientes-calificar")
    public List<OperacionDtos.HistorialItemResponse> pendientesCalificar(@AuthenticationPrincipal UserDetails user) {
        return service.pendientesCalificar(user.getUsername());
    }
}
