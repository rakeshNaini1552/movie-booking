package com.movie.booking.booking_service.dto;

import jakarta.validation.constraints.NotBlank;

public record PayBookingRequest(@NotBlank String paymentToken) {}

