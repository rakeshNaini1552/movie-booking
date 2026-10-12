package com.movie.booking.booking_service.advice;

import com.movie.booking.booking_service.dto.ApiError;
import com.movie.booking.booking_service.excepiton.SeatsUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BookingApiExceptionHandler {

    @ExceptionHandler(SeatsUnavailableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleSeatsUnavailable(SeatsUnavailableException e) {
        return new ApiError(e.getMessage());
    }
}
