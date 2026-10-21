package com.movie.booking.booking_service.dto;

import com.movie.booking.booking_service.BookingStatus;

import java.util.UUID;

public record CancelBookingResponse(UUID bookingId, BookingStatus status) {}
