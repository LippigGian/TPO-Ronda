package com.ronda.backend.publicacion;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/publicaciones")
public class PublicacionController {
    private final PublicacionService service;

    public PublicacionController(PublicacionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionDtos.Response create(@AuthenticationPrincipal UserDetails user,
                                           @Valid @RequestBody PublicacionDtos.CreateRequest request) {
        return service.create(user.getUsername(), request);
    }

    @GetMapping
    public PublicacionDtos.PageResponse<PublicacionDtos.Resumen> explorar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) EstadoArticulo estadoArticulo,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radioKm,
            @RequestParam(defaultValue = "RECIENTES") OrdenPublicaciones orden,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var filtros = new ExplorarFiltros(q, categoria, precioMin, precioMax, estadoArticulo, lat, lng, radioKm);
        return service.explorar(filtros, orden, page, size);
    }

    @GetMapping("/categorias")
    public List<String> categorias() {
        return service.categorias();
    }

    @GetMapping("/{id}")
    public PublicacionDtos.Detalle detalle(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return service.detalle(user.getUsername(), id);
    }

    @GetMapping("/mias")
    public List<PublicacionDtos.Response> listMine(@AuthenticationPrincipal UserDetails user) {
        return service.listMine(user.getUsername());
    }

    @PatchMapping("/{id}/estado")
    public PublicacionDtos.Response changeStatus(@AuthenticationPrincipal UserDetails user, @PathVariable Long id,
                                                  @Valid @RequestBody PublicacionDtos.ChangeStatusRequest request) {
        return service.changeStatus(user.getUsername(), id, request);
    }

    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PublicacionDtos.Response addPhotos(@AuthenticationPrincipal UserDetails user, @PathVariable Long id,
                                              @RequestPart("fotos") List<MultipartFile> fotos) {
        return service.addPhotos(user.getUsername(), id, fotos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        service.delete(user.getUsername(), id);
    }
}
