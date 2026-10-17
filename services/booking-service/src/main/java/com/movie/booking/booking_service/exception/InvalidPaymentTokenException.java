package com.movie.booking.booking_service.exception;

public class InvalidPaymentTokenException extends RuntimeException{
    public InvalidPaymentTokenException(String message){
        super(message);
    }
}
