package com.movie.booking.seat_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static com.movie.booking.seat_service.TestJwt.internalService;
import static com.movie.booking.seat_service.TestJwt.withoutInternalServiceRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class SeatServiceSecurityTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void hold_without_token_returns_401() throws Exception {
        mockMvc.perform(holdRequest(UUID.randomUUID(), "A1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hold_with_jwt_lacking_internal_service_role_returns_403() throws Exception {
        mockMvc.perform(holdRequest(UUID.randomUUID(), "A1").with(withoutInternalServiceRole()))
                .andExpect(status().isForbidden());
    }

    @Test
    void hold_with_internal_service_role_passes_security() throws Exception {
        UUID showId = UUID.randomUUID();
        insertAvailableSeat(showId, "A1");

        mockMvc.perform(holdRequest(showId, "A1").with(internalService()))
                .andExpect(status().isCreated());
    }

    @Test
    void confirm_and_release_also_require_internal_service_role() throws Exception {
        UUID showId = UUID.randomUUID();
        String body = "{\"seatIds\":[\"A1\"],\"bookingId\":\"" + UUID.randomUUID() + "\"}";
        MockHttpServletRequestBuilder confirm = post("/api/v1/shows/{showId}/bookings", showId)
                .contentType(MediaType.APPLICATION_JSON).content(body);
        MockHttpServletRequestBuilder release = post("/api/v1/shows/{showId}/holds/{bookingId}/release",
                showId, UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("{\"seatIds\":[\"A1\"]}");

        mockMvc.perform(confirm).andExpect(status().isUnauthorized());
        mockMvc.perform(release).andExpect(status().isUnauthorized());
        mockMvc.perform(confirm.with(withoutInternalServiceRole())).andExpect(status().isForbidden());
        mockMvc.perform(release.with(withoutInternalServiceRole())).andExpect(status().isForbidden());
    }

    @Test
    void seat_map_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/shows/{showId}/seats", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void seat_map_needs_only_an_authenticated_user_not_the_internal_service_role() throws Exception {
        UUID showId = UUID.randomUUID();
        insertAvailableSeat(showId, "A1");

        mockMvc.perform(get("/api/v1/shows/{showId}/seats", showId).with(withoutInternalServiceRole()))
                .andExpect(status().isOk());
    }

    @Test
    void seat_map_is_also_open_to_internal_service() throws Exception {
        UUID showId = UUID.randomUUID();
        insertAvailableSeat(showId, "A1");

        mockMvc.perform(get("/api/v1/shows/{showId}/seats", showId).with(internalService()))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder holdRequest(UUID showId, String seatId) {
        return post("/api/v1/shows/{showId}/holds", showId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatIds\":[\"%s\"],\"bookingId\":\"%s\"}".formatted(seatId, UUID.randomUUID()));
    }

    private void insertAvailableSeat(UUID showId, String seatId) {
        jdbcTemplate.update(
                "INSERT INTO show_seat (show_id, seat_id, seat_row, seat_number, seat_type, status) VALUES (?, ?, ?, ?, ?, ?)",
                showId, seatId, seatId.substring(0, 1), Integer.parseInt(seatId.substring(1)),
                SeatType.GOLD.name(), SeatStatus.AVAILABLE.name());
    }
}
