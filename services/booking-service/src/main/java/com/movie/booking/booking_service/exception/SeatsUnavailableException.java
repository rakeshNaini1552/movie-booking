package com.movie.booking.booking_service.exception;

public class SeatsUnavailableException extends RuntimeException {
    public SeatsUnavailableException(String message) { super(message); }
}