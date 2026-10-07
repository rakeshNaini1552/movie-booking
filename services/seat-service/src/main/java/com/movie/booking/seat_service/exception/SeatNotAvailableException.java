package com.movie.booking.seat_service.exception;

import com.movie.booking.seat_service.ShowSeatId;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class SeatNotAvailableException extends RuntimeException {

    private final UUID showId;
    private final List<String> seatIds;

    public SeatNotAvailableException(UUID id, List<String> seatIds) {
        super("Seat " + id + " no longer available");
        this.showId = id;
        this.seatIds = seatIds;
    }
}
