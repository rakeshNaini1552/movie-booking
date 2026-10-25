package com.movie.booking.ticketservice.event;

import java.time.Instant;
import java.util.UUID;

public record TicketScannedEvent(UUID eventId,
        Instant occurredAt,
        UUID ticketId,
        UUID bookingId
){
}
