package com.movie.booking.ticketservice.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingCancelledEvent(
        UUID eventId,
        Instant occurredAt,
        UUID bookingId,
        UUID userId,
        UUID showId,
        List<String> seatIds
) {}
