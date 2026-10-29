package com.movie.booking.payment_service.dto;

import com.movie.booking.payment_service.entity.PaymentStatus;

import java.util.UUID;

public record PaymentResponse(UUID paymentId, PaymentStatus status) {
}
