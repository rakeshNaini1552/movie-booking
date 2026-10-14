package com.movie.booking.booking_service.exception;

public class SeatServiceUnavailableException extends RuntimeException {
    public SeatServiceUnavailableException(String s, Throwable e) {
        super(s, e);
    }
}
