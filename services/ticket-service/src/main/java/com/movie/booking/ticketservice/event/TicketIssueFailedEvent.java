package com.movie.booking.ticketservice.event;

import java.time.Instant;
import java.util.UUID;

public record TicketIssueFailedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID bookingId,
        UUID userId,
        String reason
) {
}
