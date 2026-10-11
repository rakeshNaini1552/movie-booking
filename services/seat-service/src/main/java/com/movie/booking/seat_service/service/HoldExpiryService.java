package com.movie.booking.seat_service.service;

import com.movie.booking.seat_service.event.SeatEventPublisher;
import com.movie.booking.seat_service.event.SeatsReleased;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

@Service
public class HoldExpiryService {

    private final HoldStore holdStore;
    private final SeatEventPublisher eventPublisher;
    private final Clock clock;
    private final int batchSize;

    public HoldExpiryService(HoldStore holdStore,
                             SeatEventPublisher eventPublisher,
                             Clock clock,
                             @Value("${seat.hold.expiry.batch-size}") int batchSize) {
        this.holdStore = holdStore;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    /** Release every hold that is due and report each one. Returns how many were released. */
    public int releaseExpiredHolds() {
        int released = 0;
        List<ExpiredHold> batch;
        do {
            batch = holdStore.claimExpired(clock.instant(), batchSize);
            for (ExpiredHold hold : batch) {
                holdStore.release(hold.showId(), hold.seatIds(), hold.bookingId());
                eventPublisher.publish(new SeatsReleased(
                        hold.showId(), hold.seatIds(), hold.bookingId(), SeatsReleased.Reason.EXPIRED));
            }
            released += batch.size();
        } while (batch.size() == batchSize);
        return released;
    }
}
