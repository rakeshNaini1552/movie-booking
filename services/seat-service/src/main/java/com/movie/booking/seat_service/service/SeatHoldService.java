package com.movie.booking.seat_service.service;

import com.movie.booking.seat_service.SeatStatus;
import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final ShowSeatRepository showSeatRepository;
    private final HoldStore holdStore;
    private final Duration holdTtl;          // comes from config, see step 3

    public void holdSeats(UUID showId, List<String> seatIds, UUID bookingId) {
        long available = showSeatRepository.countByIdShowIdAndIdSeatIdInAndStatus(showId, seatIds, SeatStatus.AVAILABLE);
        if (available != seatIds.size()) {
            throw new SeatNotAvailableException(showId, seatIds);
        }

        holdStore.hold(showId, seatIds, bookingId, holdTtl);
    }

    public void extendHold(UUID showId, List<String> seatIds, UUID bookingId) {
        boolean extended = holdStore.extend(showId, seatIds, bookingId, holdTtl);   // new TTL
        if (!extended) {
            throw new SeatNotAvailableException(showId, seatIds);  // which exception?
        }
    }

    public void releaseHold(UUID showId, List<String> seatIds, UUID bookingId) {
        holdStore.release(showId, seatIds, bookingId);
    }
}
