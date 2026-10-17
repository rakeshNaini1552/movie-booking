package com.movie.booking.booking_service.dto;

import com.movie.booking.booking_service.BookingStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BookingResponse(UUID bookingId, UUID showId, BookingStatus status,
                              BigDecimal totalAmount, List<String> seatIds) {}
