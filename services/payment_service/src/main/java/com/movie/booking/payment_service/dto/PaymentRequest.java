package com.movie.booking.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        @NotNull UUID bookingId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String paymentToken) {
}
