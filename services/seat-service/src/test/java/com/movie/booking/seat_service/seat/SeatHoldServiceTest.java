package com.movie.booking.seat_service.seat;

import com.movie.booking.seat_service.SeatStatus;
import com.movie.booking.seat_service.SeatType;
import com.movie.booking.seat_service.TestContainersConfig;
import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.service.HoldStore;
import com.movie.booking.seat_service.service.SeatBookingService;
import com.movie.booking.seat_service.service.SeatHoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
public class SeatHoldServiceTest {

    @Autowired
    private SeatHoldService seatHoldService;
    @Autowired
    private HoldStore holdStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SeatBookingService service;

    private void insertSeat(UUID showId, String seatId, SeatStatus status) {
        jdbcTemplate.update(
                "INSERT INTO show_seat (show_id, seat_id, seat_row, seat_number, seat_type, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                showId,
                seatId,
                seatId.substring(0, 1),                     // "A1" -> "A"
                Integer.parseInt(seatId.substring(1)),      // "A1" -> 1
                SeatType.GOLD.name(),                                       // a SeatType name, e.g. SeatType.SILVER.name()
                status.name());
    }

    @Test
    void holds_seats_when_all_available() {
        // arrange: A1, A2 are AVAILABLE for showId
        UUID showId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.AVAILABLE);
        insertSeat(showId, "A2", SeatStatus.AVAILABLE);

        UUID holdId = seatHoldService.holdSeats(showId, List.of("A1", "A2"));

        assertThat(holdStore.isHeldBy(showId, List.of("A1", "A2"), holdId)).isTrue();
    }

    @Test
    void does_not_hold_anything_when_one_seat_is_booked() {
        // arrange: A1 is BOOKED, A2 is AVAILABLE

        UUID showId = UUID.randomUUID();

        insertSeat(showId, "A1", SeatStatus.BOOKED);
        insertSeat(showId, "A2", SeatStatus.AVAILABLE);

        assertThatThrownBy(() -> seatHoldService.holdSeats(showId, List.of("A1", "A2")))
                .isInstanceOf(SeatNotAvailableException.class);

        // A2 must NOT be held. How do you check that? (hint: which HoldStore method?)
    }

    @Test
    void second_user_cannot_hold_same_seats() {

        UUID showId = UUID.randomUUID();
        insertSeat(showId, "A3", SeatStatus.AVAILABLE);

        seatHoldService.holdSeats(showId, List.of("A3"));

        assertThatThrownBy(() -> seatHoldService.holdSeats(showId, List.of("A3")))
                .isInstanceOf(SeatNotAvailableException.class);
    }
}
