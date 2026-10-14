package com.movie.booking.booking_service.advice;

import com.movie.booking.booking_service.dto.ApiError;
import com.movie.booking.booking_service.exception.SeatServiceUnavailableException;
import com.movie.booking.booking_service.exception.SeatsUnavailableException;
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

    @ExceptionHandler(SeatServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleSeatServiceUnavailable(SeatServiceUnavailableException e) {
        return new ApiError("Seat service is temporarily unavailable, please try again");
    }
}
