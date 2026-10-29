package com.movie.booking.payment_service.exception;

public class InvalidPaymentTokenException extends RuntimeException {
    public InvalidPaymentTokenException(String message){
        super(message);
    }
}
