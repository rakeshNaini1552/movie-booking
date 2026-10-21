package com.movie.booking.ticketservice.dto;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.entity.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(UUID ticketId, UUID bookingId, TicketStatus status,
                             String qrPayload, Instant issuedAt, Instant usedAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(ticket.getTicketId(), ticket.getBookingId(), ticket.getStatus(),
                ticket.getQrPayload(), ticket.getIssuedAt(), ticket.getUsedAt());
    }
}
