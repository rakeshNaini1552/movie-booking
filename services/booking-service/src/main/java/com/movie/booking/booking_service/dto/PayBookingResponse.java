package com.movie.booking.booking_service.dto;

import com.movie.booking.booking_service.BookingStatus;
import com.movie.booking.booking_service.PaymentStatus;

import java.util.UUID;

public record PayBookingResponse(UUID bookingId, BookingStatus status, PaymentStatus paymentStatus) {}