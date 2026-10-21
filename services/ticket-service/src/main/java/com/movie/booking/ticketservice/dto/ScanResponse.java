package com.movie.booking.ticketservice.dto;

import com.movie.booking.ticketservice.entity.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record ScanResponse(UUID ticketId, TicketStatus status, Instant usedAt) {
}
