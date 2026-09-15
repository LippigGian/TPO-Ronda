package com.ronda.backend.status;

import com.ronda.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatusController.class)
@Import(SecurityConfig.class)
class StatusControllerTest {
    @Autowired
    MockMvc mvc;

    @Test
    void statusIsPublicAndDoesNotCreateASession() throws Exception {
        mvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("ronda-backend"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void otherEndpointsAreClosedUntilTheFeatureDefinesItsPermissions() throws Exception {
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios").with(user("test"))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/status")).andExpect(status().isForbidden());
    }
}
