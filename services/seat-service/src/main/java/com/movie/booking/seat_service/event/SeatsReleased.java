package com.movie.booking.seat_service.event;

import java.util.List;
import java.util.UUID;

public record SeatsReleased(UUID showId, List<String> seatIds, UUID bookingId, Reason reason) {

    public enum Reason {
        EXPIRED
    }
}
