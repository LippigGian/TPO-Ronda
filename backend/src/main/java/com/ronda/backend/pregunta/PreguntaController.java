package com.ronda.backend.pregunta;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/** Preguntas y respuestas (punto 4). Requieren JWT como el resto de la API. */
@RestController
@RequestMapping("/api/v1")
public class PreguntaController {
    private final PreguntaService service;

    public PreguntaController(PreguntaService service) {
        this.service = service;
    }

    @GetMapping("/publicaciones/{publicacionId}/preguntas")
    public List<PreguntaDtos.Response> listar(@AuthenticationPrincipal UserDetails user,
                                              @PathVariable Long publicacionId) {
        return service.listar(user.getUsername(), publicacionId);
    }

    @PostMapping("/publicaciones/{publicacionId}/preguntas")
    @ResponseStatus(HttpStatus.CREATED)
    public PreguntaDtos.Response preguntar(@AuthenticationPrincipal UserDetails user, @PathVariable Long publicacionId,
                                           @Valid @RequestBody PreguntaDtos.PreguntarRequest request) {
        return service.preguntar(user.getUsername(), publicacionId, request);
    }

    @PostMapping("/preguntas/{id}/respuesta")
    public PreguntaDtos.Response responder(@AuthenticationPrincipal UserDetails user, @PathVariable Long id,
                                           @Valid @RequestBody PreguntaDtos.ResponderRequest request) {
        return service.responder(user.getUsername(), id, request);
    }
}
