package com.movie.booking.booking_service.exception;

public class PaymentServiceUnavailableException extends RuntimeException{
    public PaymentServiceUnavailableException(String message, Throwable e){
        super(message, e);
    }
}
