package com.movie.booking.booking_service.event;

import java.time.Instant;
import java.util.UUID;

public record TicketScannedEvent(UUID eventId,
                                 Instant occurredAt,
                                 UUID ticketId,
                                 UUID bookingId
){
}
