package com.ronda.backend.operacion;

import com.ronda.backend.auth.AuthService;
import com.ronda.backend.auth.JwtService;
import com.ronda.backend.config.SecurityConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OperacionController.class)
@Import(SecurityConfig.class)
class OperacionControllerTest {
    @MockitoBean
    JwtService jwtService;
    @MockitoBean
    AuthService authService;
    @MockitoBean
    OperacionService service;

    @Autowired
    MockMvc mvc;

    private OperacionDtos.HistorialItemResponse item() {
        return new OperacionDtos.HistorialItemResponse(1L, "VENTA", 2L, "Bici", new BigDecimal("100.00"),
                Instant.parse("2024-01-01T00:00:00Z"), 3L, "comprador", false, Instant.parse("2024-01-08T00:00:00Z"));
    }

    @Test
    void confirmarRecepcionRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/operaciones/confirmar-recepcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicacionId\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmarRecepcionReturnsCreatedOperation() throws Exception {
        when(service.confirmarRecepcion(eq("comprador@test.com"), any())).thenReturn(item());

        mvc.perform(post("/api/v1/operaciones/confirmar-recepcion")
                        .with(user("comprador@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicacionId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicacionTitulo").value("Bici"))
                .andExpect(jsonPath("$.contraparteNombre").value("comprador"));
    }

    @Test
    void confirmarRecepcionRejectsMissingPublicacionIdWithReadableMessage() throws Exception {
        mvc.perform(post("/api/v1/operaciones/confirmar-recepcion")
                        .with(user("comprador@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Código inválido: el QR no indica la publicación"));
    }

    @Test
    void confirmarRecepcionRejectsNonPositivePublicacionId() throws Exception {
        mvc.perform(post("/api/v1/operaciones/confirmar-recepcion")
                        .with(user("comprador@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicacionId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Código inválido: no es un QR de entrega de Ronda"));
    }

    @Test
    void confirmarRecepcionRejectsNonNumericPublicacionId() throws Exception {
        mvc.perform(post("/api/v1/operaciones/confirmar-recepcion")
                        .with(user("comprador@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicacionId\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Código inválido: no es un QR de entrega de Ronda"));
    }

    @Test
    void confirmarRecepcionExposesBusinessErrorDetail() throws Exception {
        when(service.confirmarRecepcion(eq("comprador@test.com"), any())).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Código inválido: no corresponde a ninguna publicación"));

        mvc.perform(post("/api/v1/operaciones/confirmar-recepcion")
                        .with(user("comprador@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicacionId\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Código inválido: no corresponde a ninguna publicación"));
    }

    @Test
    void historialReturnsListForAuthenticatedUser() throws Exception {
        when(service.historial(eq("user@test.com"), eq(TipoOperacion.TODAS), isNull(), isNull()))
                .thenReturn(List.of(item()));

        mvc.perform(get("/api/v1/operaciones/historial").with(user("user@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publicacionTitulo").value("Bici"));
    }

    @Test
    void pendientesCalificarRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/operaciones/pendientes-calificar")).andExpect(status().isForbidden());
    }
}
