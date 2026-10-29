package com.movie.booking.payment_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID bookingId,
        UUID userId,
        BigDecimal amount
) {}
