package com.movie.booking.booking_service.client;

import com.movie.booking.booking_service.dto.PaymentResult;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentClient {
    PaymentResult pay(UUID bookingId, BigDecimal amount, String paymentToken, UUID idempotencyKey);
}
