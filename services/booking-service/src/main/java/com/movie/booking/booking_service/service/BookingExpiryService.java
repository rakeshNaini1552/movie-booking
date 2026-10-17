package com.movie.booking.booking_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class BookingExpiryService {

    private final BookingStateService bookingStateService;
    private final Clock clock;
    private final Duration pendingTimeout;

    public BookingExpiryService(BookingStateService bookingStateService,
                                Clock clock,
                                @Value("${booking.expiry.pending-timeout}") Duration pendingTimeout) {
        this.bookingStateService = bookingStateService;
        this.clock = clock;
        this.pendingTimeout = pendingTimeout;
    }

    public int expireStaleBookings() {
        Instant cutoff = clock.instant().minus(pendingTimeout);
        int expired = 0;
        for (UUID bookingId : bookingStateService.findStalePendingIds(cutoff)) {
            try {
                if (bookingStateService.markExpired(bookingId)) {
                    expired++;
                }
            } catch (RuntimeException e) {
                log.warn("Could not expire booking {}: {}", bookingId, e.getMessage());
            }
        }
        if (expired > 0) {
            log.info("Expired {} stale PENDING bookings", expired);
        }
        return expired;
    }
}