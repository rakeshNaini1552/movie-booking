package com.movie.booking.booking_service.exception;

public class SeatConfirmationFailedException extends RuntimeException {
    public SeatConfirmationFailedException(String message) {
        super(message);
    }
}
