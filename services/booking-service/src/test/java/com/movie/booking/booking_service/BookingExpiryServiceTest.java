package com.movie.booking.booking_service;

import com.movie.booking.booking_service.service.BookingExpiryService;
import com.movie.booking.booking_service.service.BookingStateService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingExpiryServiceTest {

    private final BookingStateService bookingStateService = mock(BookingStateService.class);
    private final Instant now = Instant.parse("2026-10-16T12:00:00Z");
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BookingExpiryService service = new BookingExpiryService(
            bookingStateService, Clock.fixed(now, ZoneOffset.UTC), Duration.ofMinutes(10), registry);

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @Test
    void searches_for_bookings_older_than_the_timeout() {
        when(bookingStateService.findStalePendingIds(any())).thenReturn(List.of());

        service.expireStaleBookings();

        verify(bookingStateService).findStalePendingIds(now.minus(Duration.ofMinutes(10)));
    }

    @Test
    void expires_every_stale_booking_and_returns_the_count() {
        when(bookingStateService.findStalePendingIds(any())).thenReturn(List.of(first, second));
        when(bookingStateService.markExpired(first)).thenReturn(true);
        when(bookingStateService.markExpired(second)).thenReturn(true);

        assertThat(service.expireStaleBookings()).isEqualTo(2);
    }

    @Test
    void does_not_count_a_booking_that_was_no_longer_pending() {
        when(bookingStateService.findStalePendingIds(any())).thenReturn(List.of(first));
        when(bookingStateService.markExpired(first)).thenReturn(false);

        assertThat(service.expireStaleBookings()).isZero();
    }

    @Test
    void one_failing_booking_does_not_stop_the_rest() {
        when(bookingStateService.findStalePendingIds(any())).thenReturn(List.of(first, second));
        when(bookingStateService.markExpired(first)).thenThrow(new RuntimeException("db down"));
        when(bookingStateService.markExpired(second)).thenReturn(true);

        assertThat(service.expireStaleBookings()).isEqualTo(1);
        verify(bookingStateService).markExpired(second);
    }

    @Test
    void counter_stays_at_zero_when_nothing_expired() {
        when(bookingStateService.findStalePendingIds(any())).thenReturn(List.of());

        service.expireStaleBookings();

        assertThat(registry.counter("hold.expirations").count()).isEqualTo(0.0);
    }

    @Test
    void counter_increases_by_the_number_of_rows_actually_expired() {
        UUID third = UUID.randomUUID();
        UUID lost = UUID.randomUUID();
        when(bookingStateService.findStalePendingIds(any())).thenReturn(List.of(first, second, third, lost));
        when(bookingStateService.markExpired(first)).thenReturn(true);
        when(bookingStateService.markExpired(second)).thenReturn(true);
        when(bookingStateService.markExpired(third)).thenReturn(true);
        when(bookingStateService.markExpired(lost)).thenReturn(false);

        service.expireStaleBookings();

        assertThat(registry.counter("hold.expirations").count()).isEqualTo(3.0);
    }
}
