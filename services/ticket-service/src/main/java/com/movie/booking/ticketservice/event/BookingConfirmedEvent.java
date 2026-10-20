package com.movie.booking.ticketservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingConfirmedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID bookingId,
        UUID userId,
        UUID showId,
        List<String> seatIds
) {}
