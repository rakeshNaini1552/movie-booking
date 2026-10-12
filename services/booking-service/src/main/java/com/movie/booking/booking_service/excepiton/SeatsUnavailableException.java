package com.movie.booking.booking_service.excepiton;

public class SeatsUnavailableException extends RuntimeException {
    public SeatsUnavailableException(String message) { super(message); }
}