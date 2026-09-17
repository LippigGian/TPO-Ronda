package com.ronda.backend.publicacion;

import jakarta.validation.Valid;
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
}
