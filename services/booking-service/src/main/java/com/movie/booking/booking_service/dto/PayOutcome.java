package com.movie.booking.booking_service.dto;

import com.movie.booking.booking_service.BookingStatus;
import com.movie.booking.booking_service.PaymentStatus;

public record PayOutcome(BookingStatus bookingStatus, PaymentStatus paymentStatus) {}