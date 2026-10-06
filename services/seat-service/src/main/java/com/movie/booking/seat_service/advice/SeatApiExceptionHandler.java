package com.movie.booking.seat_service.advice;

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
}
