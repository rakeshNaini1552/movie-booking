package com.movie.booking.seat_service.exception;

import com.movie.booking.seat_service.ShowSeatId;
import lombok.Getter;

@Getter
public class SeatNotAvailableException extends RuntimeException {

    private final ShowSeatId id;

    public SeatNotAvailableException(ShowSeatId id) {
        super("Seat " + id + " no longer available");
        this.id = id;
    }
}
