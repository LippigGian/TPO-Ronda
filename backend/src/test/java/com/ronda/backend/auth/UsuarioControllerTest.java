package com.ronda.backend.auth;

import com.ronda.backend.config.SecurityConfig;
import com.ronda.backend.operacion.CalificacionDtos;
import com.ronda.backend.operacion.ReputacionDtos;
import com.ronda.backend.operacion.ReputacionService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {
    @MockitoBean
    JwtService jwtService;
    @MockitoBean
    AuthService authService;
    @MockitoBean
    UsuarioRepository usuarios;
    @MockitoBean
    ReputacionService reputacionService;

    @Autowired
    MockMvc mvc;

    @Test
    void buscarRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/usuarios/buscar").param("email", "a@test.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscarReturnsPublicDataWhenFound() throws Exception {
        Usuario usuario = new Usuario("comprador@test.com", "hash", "comprador");
        when(usuarios.findByEmailIgnoreCase("comprador@test.com")).thenReturn(Optional.of(usuario));

        mvc.perform(get("/api/v1/usuarios/buscar")
                        .param("email", "comprador@test.com")
                        .with(user("vendedor@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("comprador"));
    }

    @Test
    void buscarReturnsNotFoundWhenMissing() throws Exception {
        when(usuarios.findByEmailIgnoreCase("nadie@test.com")).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/usuarios/buscar")
                        .param("email", "nadie@test.com")
                        .with(user("vendedor@test.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarRejectsInvalidEmail() throws Exception {
        mvc.perform(get("/api/v1/usuarios/buscar")
                        .param("email", "no-es-email")
                        .with(user("vendedor@test.com")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reputacionReturnsAggregatedData() throws Exception {
        when(reputacionService.obtener(5L)).thenReturn(new ReputacionDtos.Response(5L, "vendedor", 4.5, 2,
                List.of(new CalificacionDtos.Response(1L, 1L, "comprador", "vendedor", 5, "Excelente",
                        Instant.parse("2024-01-02T00:00:00Z")))));

        mvc.perform(get("/api/v1/usuarios/5/reputacion").with(user("comprador@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promedio").value(4.5))
                .andExpect(jsonPath("$.ultimasCalificaciones[0].puntaje").value(5));
    }
}
