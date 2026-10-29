package com.movie.booking.payment_service.advice;

import com.movie.booking.payment_service.dto.ApiError;
import com.movie.booking.payment_service.exception.InvalidPaymentTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentApiExceptionHandler {
    @ExceptionHandler(InvalidPaymentTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidToken(InvalidPaymentTokenException e) {
        return new ApiError("Invalid payment token");
    }
}
