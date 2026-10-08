package com.movie.booking.seat_service.seat;

import com.jayway.jsonpath.JsonPath;
import com.movie.booking.seat_service.SeatStatus;
import com.movie.booking.seat_service.SeatType;
import com.movie.booking.seat_service.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class SeatBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void insertSeat(UUID showId, String seatId, SeatStatus status) {
        jdbcTemplate.update(
                "INSERT INTO show_seat (show_id, seat_id, seat_row, seat_number, seat_type, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                showId,
                seatId,
                seatId.substring(0, 1),
                Integer.parseInt(seatId.substring(1)),
                SeatType.GOLD.name(),
                status.name());
    }

    private String holdSeats(UUID showId, String seatId) throws Exception {
        String json = mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\": [\"%s\"]}".formatted(seatId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.holdId");
    }

    private String confirmBody(String seatId, Object holdId, UUID bookingId) {
        return """
                {"seatIds": ["%s"], "holdId": "%s", "bookingId": "%s"}
                """.formatted(seatId, holdId, bookingId);
    }

    @Test
    void returns_201_and_books_the_seat_after_a_valid_hold() throws Exception {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.AVAILABLE);
        String holdId = holdSeats(showId, "A1");

        mockMvc.perform(post("/api/v1/shows/{showId}/bookings", showId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody("A1", holdId, bookingId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()));

        String seatStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM show_seat WHERE show_id = ? AND seat_id = 'A1'",
                String.class, showId);
        assertThat(seatStatus).isEqualTo("BOOKED");
    }

    @Test
    void returns_409_when_the_hold_does_not_exist() throws Exception {
        UUID showId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.AVAILABLE);

        mockMvc.perform(post("/api/v1/shows/{showId}/bookings", showId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody("A1", UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isConflict());

        String seatStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM show_seat WHERE show_id = ? AND seat_id = 'A1'",
                String.class, showId);
        assertThat(seatStatus).isEqualTo("AVAILABLE");
    }

    @Test
    void returns_400_when_the_body_is_invalid() throws Exception {
        mockMvc.perform(post("/api/v1/shows/{showId}/bookings", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\": []}"))
                .andExpect(status().isBadRequest());
    }
}