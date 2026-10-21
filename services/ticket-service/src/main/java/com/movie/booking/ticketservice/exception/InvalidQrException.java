package com.movie.booking.ticketservice.exception;

public class InvalidQrException extends RuntimeException {
    public InvalidQrException(String message) {
        super(message);
    }
}
