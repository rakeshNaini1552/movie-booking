package com.movie.booking.seat_service.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface HoldStore {

    /** Hold all the seats or none. Fails if any seat is already held. */
    void hold(UUID showId, List<String> seatIds, UUID holdId, Duration ttl);

    /** Is every one of these seats currently held by this holdId? */
    boolean isHeldBy(UUID showId, List<String> seatIds, UUID holdId);

    /** Free the seats early, but only if this holdId owns them. */
    void release(UUID showId, List<String> seatIds, UUID holdId);

    /** Reset the TTL of all these seats, but only if this holdId still owns every one. */
    boolean extend(UUID showId, List<String> seatIds, UUID holdId, Duration ttl);

    List<String> getHeldSeats(UUID showId, List<String> seatIds);
}
