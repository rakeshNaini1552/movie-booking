package com.movie.booking.booking_service.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingConfirmedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID bookingId,
        UUID userId,
        UUID showId,
        List<String> seatIds
) {}