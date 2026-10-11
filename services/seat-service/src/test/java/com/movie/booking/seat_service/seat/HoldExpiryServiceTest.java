package com.movie.booking.seat_service.seat;

import com.movie.booking.seat_service.RecordingSeatEventPublisher;
import com.movie.booking.seat_service.SeatStatus;
import com.movie.booking.seat_service.SeatType;
import com.movie.booking.seat_service.TestContainersConfig;
import com.movie.booking.seat_service.event.SeatEventPublisher;
import com.movie.booking.seat_service.event.SeatsReleased;
import com.movie.booking.seat_service.service.HoldExpiryService;
import com.movie.booking.seat_service.service.HoldStore;
import com.movie.booking.seat_service.service.SeatConfirmService;
import com.movie.booking.seat_service.service.SeatHoldService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "seat.hold.ttl=1s")
@Import({TestContainersConfig.class, HoldExpiryServiceTest.RecordingConfig.class})
class HoldExpiryServiceTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingConfig {
        @Bean
        @Primary
        RecordingSeatEventPublisher recordingPublisher() {
            return new RecordingSeatEventPublisher();
        }
    }

    @Autowired
    private HoldExpiryService holdExpiryService;
    @Autowired
    private SeatHoldService seatHoldService;
    @Autowired
    private SeatConfirmService seatConfirmService;
    @Autowired
    private HoldStore holdStore;
    @Autowired
    private RecordingSeatEventPublisher publisher;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void insertSeat(UUID showId, String seatId) {
        jdbcTemplate.update(
                "INSERT INTO show_seat (show_id, seat_id, seat_row, seat_number, seat_type, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                showId, seatId, seatId.substring(0, 1), Integer.parseInt(seatId.substring(1)),
                SeatType.GOLD.name(), SeatStatus.AVAILABLE.name());
    }

    @Test
    void releases_an_expired_hold_and_reports_it() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        insertSeat(showId, "A1");
        insertSeat(showId, "A2");
        seatHoldService.holdSeats(showId, List.of("A1", "A2"), bookingId);

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            holdExpiryService.releaseExpiredHolds();
            assertThat(publisher.eventsFor(showId)).containsExactly(
                    new SeatsReleased(showId, List.of("A1", "A2"), bookingId, SeatsReleased.Reason.EXPIRED));
        });

        assertThat(holdStore.getHeldSeats(showId, List.of("A1", "A2"))).isEmpty();
    }

    @Test
    void a_confirmed_booking_is_not_reported_as_expired() throws InterruptedException {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        insertSeat(showId, "A1");
        seatHoldService.holdSeats(showId, List.of("A1"), bookingId);
        seatConfirmService.confirm(showId, List.of("A1"), bookingId);

        Thread.sleep(1500);   // longer than the 1s hold TTL
        holdExpiryService.releaseExpiredHolds();

        assertThat(publisher.eventsFor(showId)).isEmpty();
    }

    @Test
    void a_released_hold_is_not_reported_as_expired() throws InterruptedException {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        insertSeat(showId, "A1");
        seatHoldService.holdSeats(showId, List.of("A1"), bookingId);
        seatHoldService.releaseHold(showId, List.of("A1"), bookingId);

        Thread.sleep(1500);
        holdExpiryService.releaseExpiredHolds();

        assertThat(publisher.eventsFor(showId)).isEmpty();
    }
}
