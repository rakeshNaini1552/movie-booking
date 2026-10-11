package com.movie.booking.seat_service.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HoldStore {

    /** Hold all the seats or none. Fails if any seat is already held. */
    void hold(UUID showId, List<String> seatIds, UUID bookingId, Duration ttl);

    /** Is every one of these seats currently held by this bookingId? */
    boolean isHeldBy(UUID showId, List<String> seatIds, UUID bookingId);

    /** Free the seats early, but only if this bookingId owns them. */
    void release(UUID showId, List<String> seatIds, UUID bookingId);

    /** Reset the TTL of all these seats, but only if this bookingId still owns every one. */
    boolean extend(UUID showId, List<String> seatIds, UUID bookingId, Duration ttl);

    /**
     * Take up to {@code limit} holds whose expiry time is at or before {@code now}. Each hold is
     * returned to one caller only, and is no longer tracked for expiry afterwards.
     */
    List<ExpiredHold> claimExpired(Instant now, int limit);

    List<String> getHeldSeats(UUID showId, List<String> seatIds);
}
