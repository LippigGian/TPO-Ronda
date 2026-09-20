package com.ronda.backend.favorito;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/busquedas-guardadas")
public class BusquedaGuardadaController {
    private final BusquedaGuardadaService service;

    public BusquedaGuardadaController(BusquedaGuardadaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusquedaGuardadaDtos.Item crear(@AuthenticationPrincipal UserDetails user,
                                            @Valid @RequestBody BusquedaGuardadaDtos.CrearRequest request) {
        return service.guardar(user.getUsername(), request);
    }

    @GetMapping
    public List<BusquedaGuardadaDtos.Item> listar(@AuthenticationPrincipal UserDetails user) {
        return service.listar(user.getUsername());
    }

    @PostMapping("/{id}/marcar-vista")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarVista(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        service.marcarVista(user.getUsername(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        service.eliminar(user.getUsername(), id);
    }
}
