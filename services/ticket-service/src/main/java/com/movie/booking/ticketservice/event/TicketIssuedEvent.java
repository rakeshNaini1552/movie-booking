package com.movie.booking.ticketservice.event;

import java.time.Instant;
import java.util.UUID;

public record TicketIssuedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID ticketId,
        UUID bookingId,
        UUID userId
) {
}
