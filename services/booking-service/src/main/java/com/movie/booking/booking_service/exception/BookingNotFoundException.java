package com.movie.booking.booking_service.exception;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(UUID bookingId) {
        super("Booking not found: " + bookingId);
    }
}
