package com.ronda.backend.oferta;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Devuelve los errores de ofertas con un mensaje legible en el campo "detail".
 *
 * application.yml usa include-message: never, que oculta el motivo de los errores.
 * Para ofertas el motivo es parte de la experiencia ("La oferta ya está vencida",
 * "Tenés que esperar la respuesta de la otra parte"), asi que se expone solo para
 * este controlador, sin cambiar el comportamiento del resto de la API.
 * ApiClient.errorMessage en la app ya lee el campo "detail".
 */
@RestControllerAdvice(assignableTypes = OfertaController.class)
public class OfertaErrorHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> errorDeNegocio(ResponseStatusException error) {
        String detalle = error.getReason() != null ? error.getReason() : "No se pudo completar la operación";
        return ResponseEntity.status(error.getStatusCode())
                .body(ProblemDetail.forStatusAndDetail(error.getStatusCode(), detalle));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> datosInvalidos(MethodArgumentNotValidException error) {
        String detalle = error.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(campo -> campo.getDefaultMessage())
                .orElse("Datos inválidos");
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle));
    }
}
