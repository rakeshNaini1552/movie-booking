package com.movie.booking.seat_service.seat;

import com.movie.booking.seat_service.SeatStatus;
import com.movie.booking.seat_service.SeatType;
import com.movie.booking.seat_service.TestContainersConfig;
import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.service.HoldStore;
import com.movie.booking.seat_service.service.SeatConfirmService;
import com.movie.booking.seat_service.service.SeatHoldService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
public class SeatConfirmServiceTest {

    @Autowired
    private HoldStore holdStore;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SeatConfirmService seatConfirmService;
    @Autowired
    private SeatHoldService seatHoldService;


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
    void confirm_happy_path() {
        UUID showId = UUID.randomUUID();

        insertSeat(showId, "A1", SeatStatus.AVAILABLE);
        insertSeat(showId, "A2", SeatStatus.AVAILABLE);

        List<String> seatIds = List.of("A1", "A2");

        UUID holdId = seatHoldService.holdSeats(showId, seatIds);

        seatConfirmService.confirm(showId, seatIds,holdId, UUID.randomUUID());

        Integer booked = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM show_seat WHERE show_id = ? AND seat_id IN('A1', 'A2')AND status = 'BOOKED'",
                Integer.class, showId);

        assertThat(booked).isEqualTo(2);

        assertThat(holdStore.isHeldBy(showId, seatIds, holdId)).isFalse();
    }

    @Test
    void confirm_failure_with_hold_that_doesnt_exist() {
        UUID showId = UUID.randomUUID();

        insertSeat(showId, "A1", SeatStatus.AVAILABLE);
        insertSeat(showId, "A2", SeatStatus.AVAILABLE);

        List<String> seatIds = List.of("A1", "A2");

        UUID holdId = seatHoldService.holdSeats(showId, seatIds);

        seatConfirmService.confirm(showId, seatIds,holdId, UUID.randomUUID());

        Integer booked = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM show_seat WHERE show_id = ? AND seat_id IN('A1', 'A2')AND status = 'BOOKED'",
                Integer.class, showId);

        assertThat(booked).isEqualTo(2);

        assertThat(holdStore.isHeldBy(showId, seatIds, holdId)).isFalse();
    }
}