package com.ronda.backend.operacion;

import com.ronda.backend.auth.AuthService;
import com.ronda.backend.auth.JwtService;
import com.ronda.backend.config.SecurityConfig;
import java.time.Instant;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalificacionController.class)
@Import(SecurityConfig.class)
class CalificacionControllerTest {
    @MockitoBean
    JwtService jwtService;
    @MockitoBean
    AuthService authService;
    @MockitoBean
    CalificacionService service;

    @Autowired
    MockMvc mvc;

    @Test
    void crearRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/operaciones/1/calificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puntaje\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearReturnsCreatedCalificacion() throws Exception {
        when(service.crear(eq("autor@test.com"), eq(1L), any())).thenReturn(
                new CalificacionDtos.Response(10L, 1L, "autor", "receptor", 5, "Todo bien",
                        Instant.parse("2024-01-02T00:00:00Z")));

        mvc.perform(post("/api/v1/operaciones/1/calificaciones")
                        .with(user("autor@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puntaje\":5,\"comentario\":\"Todo bien\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.puntaje").value(5));
    }

    @Test
    void crearRejectsScoreOutsideRange() throws Exception {
        mvc.perform(post("/api/v1/operaciones/1/calificaciones")
                        .with(user("autor@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puntaje\":6}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearPropagatesBusinessErrorAsHttpStatus() throws Exception {
        when(service.crear(eq("autor@test.com"), eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ya calificaste esta operación"));

        mvc.perform(post("/api/v1/operaciones/1/calificaciones")
                        .with(user("autor@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puntaje\":4}"))
                .andExpect(status().isConflict());
    }
}
