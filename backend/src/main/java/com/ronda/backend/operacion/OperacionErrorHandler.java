package com.ronda.backend.operacion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Devuelve los errores de operaciones y calificaciones con un mensaje legible en "detail",
 * igual que OfertaErrorHandler. El cierre con QR y la calificación necesitan mostrar el motivo exacto
 * ("Código inválido: ...", "Ya calificaste esta operación"), y application.yml oculta los mensajes
 * por defecto (include-message: never). ApiClient.errorMessage ya lee "detail".
 */
@RestControllerAdvice(assignableTypes = {OperacionController.class, CalificacionController.class})
public class OperacionErrorHandler {

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
