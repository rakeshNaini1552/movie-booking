package com.movie.booking.booking_service.advice;

import com.movie.booking.booking_service.dto.ApiError;
import com.movie.booking.booking_service.exception.*;
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

    @ExceptionHandler(BookingNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleBookingNotFound(BookingNotFoundException e) {
        return new ApiError("Booking not found");
    }

    @ExceptionHandler(BookingNotPayableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleBookingNotPayable(BookingNotPayableException e) {
        return new ApiError("Booking not payable");
    }

    @ExceptionHandler(BookingNotCancellableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleBookingNotCancellable(BookingNotCancellableException e) {
        return new ApiError("Booking not cancellable");
    }

    @ExceptionHandler(InvalidPaymentTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidPaymentToken(InvalidPaymentTokenException e) {
        return new ApiError("Invalid payment");
    }

    @ExceptionHandler(PaymentServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handlePaymentServiceUnavailable(PaymentServiceUnavailableException e) {
        return new ApiError("Sorry! Payment service is down......");
    }

    @ExceptionHandler(SeatConfirmationFailedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleSeatConfirmFailure(SeatConfirmationFailedException e) {
        return new ApiError("Seats cannot be confirmed, Please try again!!....");
    }

    @ExceptionHandler(TicketAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleTicketAlreadyUsed(TicketAlreadyUsedException e) {
        return new ApiError("Booking cannot be cancelled: ticket already used");
    }
}
