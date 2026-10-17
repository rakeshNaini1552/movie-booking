package com.movie.booking.booking_service.dto;

import java.util.UUID;

public record PayBookingCommand(UUID bookingId, String paymentToken, UUID idempotencyKey) {}

