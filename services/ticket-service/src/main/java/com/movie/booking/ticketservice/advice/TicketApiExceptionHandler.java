package com.movie.booking.ticketservice.advice;

import com.movie.booking.ticketservice.dto.ApiError;
import com.movie.booking.ticketservice.exception.InvalidQrException;
import com.movie.booking.ticketservice.exception.TicketNotFoundException;
import com.movie.booking.ticketservice.exception.TicketNotScannableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TicketApiExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(TicketNotFoundException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(InvalidQrException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidQr(InvalidQrException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidBody(MethodArgumentNotValidException e) {
        return new ApiError("Request body is invalid");
    }

    @ExceptionHandler(TicketNotScannableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleNotScannable(TicketNotScannableException e) {
        return new ApiError(e.getMessage());
    }
}
