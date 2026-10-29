package com.movie.booking.payment_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PayCommand(UUID bookingId, BigDecimal amount, String paymentToken, UUID idempotencyKey) {
}
