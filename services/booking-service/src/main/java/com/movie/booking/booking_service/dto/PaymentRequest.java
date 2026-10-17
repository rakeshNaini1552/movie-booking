package com.movie.booking.booking_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(UUID bookingId, BigDecimal amount, String paymentToken) {
}
