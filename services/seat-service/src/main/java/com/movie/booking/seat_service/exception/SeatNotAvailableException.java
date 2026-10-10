package com.movie.booking.seat_service.exception;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class SeatNotAvailableException extends RuntimeException {

    private final UUID showId;
    private final List<String> seatIds;

    public SeatNotAvailableException(UUID showId, List<String> seatIds) {
        super("Seat " + showId + " no longer available");
        this.showId = showId;
        this.seatIds = seatIds;
    }
}
