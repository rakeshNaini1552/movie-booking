package com.movie.booking.seat_service;

import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.service.SeatBookingService;
import com.movie.booking.seat_service.service.SeatMapService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class SeatBookingConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SeatBookingService service;

    private UUID insertSeat(String seatId) {
        UUID showId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO show_seat (show_id, seat_id, seat_row, seat_number, seat_type) "
                        + "VALUES (?, ?, ?, ?, ?)",
                showId, seatId, seatId.charAt(0), Integer.parseInt(String.valueOf(seatId.charAt(1))), SeatType.GOLD.name());
        return showId;
    }

    @Test
    void insertsSeat() {
        UUID showId = insertSeat("A1");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM show_seat WHERE show_id = ?", Integer.class, showId);
        assertThat(count).isEqualTo(1);
    }



    @Test
    void only_one_of_many_concurrent_bookings_succeeds() throws Exception {
        int threads = 100;
        UUID showId = insertSeat("A1");

        CountDownLatch ready = new CountDownLatch(threads);   // every task is waiting
        CountDownLatch startGate = new CountDownLatch(1);     // the starting gun
        AtomicInteger successes = new AtomicInteger();
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        startGate.await();
                        service.bookSeats(showId, List.of("A1"), UUID.randomUUID());
                        successes.incrementAndGet();
                    } catch (Throwable t) {
                        failures.add(t);
                    }
                });
            }
            ready.await();          // all 50 tasks are in place
            startGate.countDown();  // fire
        }                           // close() waits for all tasks to finish

        Map<String, Long> failureTypes = failures.stream()
                .collect(Collectors.groupingBy(t -> t.getClass().getSimpleName(), Collectors.counting()));
        System.out.println("successes=" + successes.get() + " failures=" + failureTypes);

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures).hasSize(threads - 1);
        assertThat(failures).allSatisfy(t -> assertThat(t).isInstanceOfAny(
                SeatNotAvailableException.class, ObjectOptimisticLockingFailureException.class));

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM show_seat WHERE show_id = ? AND seat_id = 'A1'", String.class, showId);
        Long version = jdbcTemplate.queryForObject(
                "SELECT version FROM show_seat WHERE show_id = ? AND seat_id = 'A1'", Long.class, showId);
        assertThat(status).isEqualTo("BOOKED");
        assertThat(version).isEqualTo(1L);   // updated exactly once
    }
}
