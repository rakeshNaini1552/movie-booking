package com.movie.booking.seat_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class SeatHolderControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer redis = new GenericContainer("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;


    private void insertSeat(UUID showId, String seatId, SeatStatus status) {
        jdbcTemplate.update(
                "INSERT INTO show_seat (show_id, seat_id, seat_row, seat_number, seat_type, status) VALUES (?, ?, ?, ?, ?, ?)",
                showId, seatId, seatId.substring(0, 1), Integer.parseInt(seatId.substring(1)), SeatType.GOLD.name(), status.name());
    }

    @Test
    public void returns_409_when_seat_is_booked() throws Exception {
        UUID showId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.BOOKED);
        mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\": [\"A1\"]}"))
                .andExpect(status().isConflict());
    }

    @Test
    public void returns_201_when_seat_is_booked() throws Exception {
        UUID showId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\": [\"A1\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holdId").isNotEmpty());
    }

    @Test
    public void returns_400_when_seats_are_empty() throws Exception {
        UUID showId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\": [\"A1\"]}"))
                .andExpect(status().is4xxClientError());
    }
}
