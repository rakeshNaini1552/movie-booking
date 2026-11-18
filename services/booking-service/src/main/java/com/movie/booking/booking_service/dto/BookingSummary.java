package com.movie.booking.booking_service.dto;

import com.movie.booking.booking_service.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingSummary(UUID bookingId, UUID showId, BookingStatus status,
                             BigDecimal totalAmount, Instant createdAt, List<String> seatIds) {}
