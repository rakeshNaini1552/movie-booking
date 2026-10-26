package com.movie.booking.ticketservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ActuatorHealthTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void liveness_without_token_returns_200_up() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readiness_without_token_returns_200_up() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void aggregate_health_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aggregate_health_with_valid_jwt_returns_200() throws Exception {
        mockMvc.perform(get("/actuator/health").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void probes_are_public_but_scan_still_requires_a_token() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());

        UUID ticketId = UUID.randomUUID();
        mockMvc.perform(post("/tickets/{id}/scan", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":\"" + ticketId + ".signature\"}"))
                .andExpect(status().isUnauthorized());
    }
}
