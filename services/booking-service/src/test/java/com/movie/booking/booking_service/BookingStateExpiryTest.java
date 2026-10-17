package com.movie.booking.booking_service;

import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.repository.BookingRepository;
import com.movie.booking.booking_service.service.BookingStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingStateExpiryTest {

    @Autowired
    private BookingStateService service;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID newPendingBooking() {
        return service.createPending(new CreateBookingCommand(
                UUID.randomUUID(), UUID.randomUUID(), List.of("A1"), UUID.randomUUID()));
    }

    private void makeOlderThan(UUID bookingId, Duration age) {
        jdbcTemplate.update("UPDATE booking SET created_at = ? WHERE booking_id = ?",
                Timestamp.from(Instant.now().minus(age)), bookingId);
    }

    private BookingStatus statusOf(UUID bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow().getStatus();
    }

    @Test
    void finds_only_old_pending_bookings() {
        UUID oldPending = newPendingBooking();
        makeOlderThan(oldPending, Duration.ofHours(1));
        UUID freshPending = newPendingBooking();
        UUID oldConfirmed = newPendingBooking();
        makeOlderThan(oldConfirmed, Duration.ofHours(1));
        service.markConfirmed(oldConfirmed);

        List<UUID> stale = service.findStalePendingIds(Instant.now().minus(Duration.ofMinutes(10)));

        assertThat(stale).contains(oldPending);
        assertThat(stale).doesNotContain(freshPending, oldConfirmed);
    }

    @Test
    void markExpired_changes_a_pending_booking_to_expired() {
        UUID bookingId = newPendingBooking();

        assertThat(service.markExpired(bookingId)).isTrue();
        assertThat(statusOf(bookingId)).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void markExpired_leaves_a_confirmed_booking_alone() {
        UUID bookingId = newPendingBooking();
        service.markConfirmed(bookingId);

        assertThat(service.markExpired(bookingId)).isFalse();
        assertThat(statusOf(bookingId)).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void markConfirmed_leaves_an_expired_booking_alone() {
        UUID bookingId = newPendingBooking();
        service.markExpired(bookingId);

        assertThat(service.markConfirmed(bookingId)).isFalse();
        assertThat(statusOf(bookingId)).isEqualTo(BookingStatus.EXPIRED);
    }
}