package com.movie.booking.seat_service.seat;

import com.movie.booking.seat_service.SeatStatus;
import com.movie.booking.seat_service.SeatType;
import com.movie.booking.seat_service.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
public class SeatHoldControllerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redis;


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
                        .content(holdBody("A1", UUID.randomUUID())))
                .andExpect(status().isConflict());
    }

    @Test
    public void returns_201_when_seat_is_booked() throws Exception {
        UUID showId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdBody("A1", UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
    }

    @Test
    public void holding_the_same_seats_twice_with_the_same_body_returns_201_both_times() throws Exception {
        UUID showId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.AVAILABLE);
        String body = holdBody("A1", UUID.randomUUID());

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    public void returns_409_when_a_different_booking_holds_the_seat() throws Exception {
        UUID showId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.AVAILABLE);
        holdSeat(showId, "A1", UUID.randomUUID());

        mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdBody("A1", UUID.randomUUID())))
                .andExpect(status().isConflict());
    }

    @Test
    public void returns_400_when_seats_are_empty() throws Exception {
        UUID showId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\": [], \"bookingId\": \"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void returns_400_when_booking_id_is_missing() throws Exception {
        UUID showId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\": [\"A1\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void extend_hold_returns_204_and_resets_ttl() throws Exception {
        UUID showId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.AVAILABLE);
        UUID bookingId = UUID.randomUUID();
        holdSeat(showId, "A1", bookingId);

        // shrink the TTL so we can see that extend resets it
        redis.expire("hold:" + showId + ":A1", Duration.ofSeconds(10));

        mockMvc.perform(post("/api/v1/shows/{showId}/holds/{bookingId}/extend", showId, bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"seatIds": ["A1"]}
                            """))
                .andExpect(status().isNoContent());

        Long ttl = redis.getExpire("hold:" + showId + ":A1");
        assertThat(ttl).isGreaterThan(10);
    }

    @Test
    void extend_hold_with_wrong_bookingId_returns_409() throws Exception {
        UUID showId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.AVAILABLE);
        holdSeat(showId, "A1", UUID.randomUUID());

        mockMvc.perform(post("/api/v1/shows/{showId}/holds/{bookingId}/extend", showId, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"seatIds": ["A1"]}
                            """))
                .andExpect(status().isConflict());
    }

    private String holdBody(String seatId, UUID bookingId) {
        return """
                {"seatIds": ["%s"], "bookingId": "%s"}
                """.formatted(seatId, bookingId);
    }

    private void holdSeat(UUID showId, String seatId, UUID bookingId) throws Exception {
        mockMvc.perform(post("/api/v1/shows/{showId}/holds", showId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdBody(seatId, bookingId)))
                .andExpect(status().isCreated());
    }
}
