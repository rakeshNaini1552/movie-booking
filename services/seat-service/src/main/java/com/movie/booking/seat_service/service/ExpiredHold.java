package com.movie.booking.seat_service.service;

import java.util.List;
import java.util.UUID;

/** A hold whose time ran out, as handed back by {@link HoldStore#claimExpired}. */
public record ExpiredHold(UUID showId, List<String> seatIds, UUID bookingId) {
}
