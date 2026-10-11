package com.movie.booking.seat_service.seat;

import com.movie.booking.seat_service.RecordingSeatEventPublisher;
import com.movie.booking.seat_service.SeatStatus;
import com.movie.booking.seat_service.SeatType;
import com.movie.booking.seat_service.TestContainersConfig;
import com.movie.booking.seat_service.job.HoldExpiryJob;
import com.movie.booking.seat_service.service.SeatHoldService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "seat.hold.ttl=1s",
        "seat.hold.expiry.enabled=true",
        "seat.hold.expiry.poll-interval=PT0.5S",
        "seat.hold.expiry.lock-at-least-for=PT0S"
})
@Import({TestContainersConfig.class, HoldExpiryJobTest.RecordingConfig.class})
class HoldExpiryJobTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingConfig {
        @Bean
        @Primary
        RecordingSeatEventPublisher recordingPublisher() {
            return new RecordingSeatEventPublisher();
        }
    }

    @Autowired
    private SeatHoldService seatHoldService;
    @Autowired
    private HoldExpiryJob holdExpiryJob;
    @Autowired
    private LockProvider lockProvider;
    @Autowired
    private RecordingSeatEventPublisher publisher;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private Clock clock;

    private void insertSeat(UUID showId, String seatId) {
        jdbcTemplate.update(
                "INSERT INTO show_seat (show_id, seat_id, seat_row, seat_number, seat_type, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                showId, seatId, seatId.substring(0, 1), Integer.parseInt(seatId.substring(1)),
                SeatType.GOLD.name(), SeatStatus.AVAILABLE.name());
    }

    @Test
    void scheduler_releases_expired_holds_on_its_own_and_only_while_it_holds_the_lock() throws InterruptedException {
        UUID showId = UUID.randomUUID();
        insertSeat(showId, "A1");

        // another pod is holding the job's lock: neither the scheduler nor a direct call may run
        // (the scheduler is already ticking, so it may briefly hold the lock: retry until we get it)
        AtomicReference<SimpleLock> otherPod = new AtomicReference<>();
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            Optional<SimpleLock> lock = lockProvider.lock(new LockConfiguration(
                    clock.instant(), HoldExpiryJob.LOCK_NAME, Duration.ofMinutes(1), Duration.ZERO));
            lock.ifPresent(otherPod::set);
            return lock.isPresent();
        });
        try {
            seatHoldService.holdSeats(showId, List.of("A1"), UUID.randomUUID());
            Thread.sleep(2500);   // hold expired, several scheduler ticks passed
            holdExpiryJob.releaseExpiredHolds();
            assertThat(publisher.eventsFor(showId)).isEmpty();
        } finally {
            otherPod.get().unlock();
        }

        // lock is free: the scheduler picks the expired hold up without being called
        Awaitility.await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(publisher.eventsFor(showId)).hasSize(1));
    }
}
