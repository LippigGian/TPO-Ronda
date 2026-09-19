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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    void venderRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/publicaciones/2/venta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"compradorEmail\":\"comprador@test.com\",\"montoFinal\":100.00}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void venderReturnsCreatedOperation() throws Exception {
        when(service.vender(eq("vendedor@test.com"), eq(2L), any())).thenReturn(item());

        mvc.perform(post("/api/v1/publicaciones/2/venta")
                        .with(user("vendedor@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"compradorEmail\":\"comprador@test.com\",\"montoFinal\":100.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("VENTA"))
                .andExpect(jsonPath("$.contraparteNombre").value("comprador"));
    }

    @Test
    void venderRejectsInvalidBody() throws Exception {
        mvc.perform(post("/api/v1/publicaciones/2/venta")
                        .with(user("vendedor@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"compradorEmail\":\"no-es-un-email\",\"montoFinal\":100.00}"))
                .andExpect(status().isBadRequest());
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
