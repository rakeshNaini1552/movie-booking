package com.movie.booking.seat_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.movie.booking.seat_service.TestJwt.internalService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
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
        mockMvc.perform(get("/actuator/health").with(internalService()))
                .andExpect(status().isOk());
    }

    @Test
    void probes_are_public_but_seat_holds_still_require_a_token() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/shows/{showId}/holds", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\":[\"A1\"],\"bookingId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
