package com.movie.booking.seat_service.advice;

import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.exception.ShowNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SeatApiExceptionHandler {

    @ExceptionHandler(ShowNotFoundException.class)
    public ProblemDetail handleShowNotFound(ShowNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Show Not found");
        problem.setProperty("showId", ex.getShowId());
        return problem;
    }

    @ExceptionHandler(SeatNotAvailableException.class)
    public ProblemDetail handleSeatNotAvailable(SeatNotAvailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Seat not available");
        problem.setProperty("seatId", ex.getId());
        return problem;
    }
}
