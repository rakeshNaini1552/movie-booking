package com.movie.booking.booking_service.exception;

import com.movie.booking.booking_service.BookingStatus;

import java.util.UUID;

public class BookingNotPayableException extends RuntimeException {
    public BookingNotPayableException(UUID bookingId, BookingStatus status) {
        super("Booking " + bookingId + " is " + status + ", not payable");
    }
}