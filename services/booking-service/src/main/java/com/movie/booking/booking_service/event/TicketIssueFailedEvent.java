package com.movie.booking.booking_service.event;

import java.time.Instant;
import java.util.UUID;

public record TicketIssueFailedEvent(UUID eventId,
                                     Instant occurredAt,
                                     UUID bookingId,
                                     UUID userId,
                                     String reason
) {
}
