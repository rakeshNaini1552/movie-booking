package com.movie.booking.seat_service.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShowNotFoundException extends RuntimeException {

    private final UUID showId;

    public ShowNotFoundException(UUID showId) {
        super("Show " + showId + " not found");
        this.showId = showId;
    }

}
