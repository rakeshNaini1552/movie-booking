package com.movie.booking.booking_service;

import com.movie.booking.booking_service.entity.Booking;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BookingTest {

    private Booking pendingBooking() {
        return new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("500.00"), Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void pending_booking_can_be_marked_failed() {
        Booking booking = pendingBooking();
        Instant later = Instant.parse("2026-01-01T10:05:00Z");

        booking.markFailed(later);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.FAILED);
        assertThat(booking.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void failed_booking_cannot_be_marked_failed_again() {
        Booking booking = pendingBooking();
        booking.markFailed(Instant.now());

        assertThatThrownBy(() -> booking.markFailed(Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}
