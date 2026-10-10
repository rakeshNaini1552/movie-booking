package com.movie.booking.seat_service.service;

import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.repository.ShowSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class SeatBookingService {

    private final ShowSeatRepository showSeatRepository;
    private final Clock clock;

    public SeatBookingService(ShowSeatRepository showSeatRepository, Clock clock) {
        this.showSeatRepository = showSeatRepository;
        this.clock = clock;
    }

    @Transactional
    public void bookSeats(UUID showId, List<String> seatIds, UUID bookingId) {
        int updated = showSeatRepository.bookIfAvailable(showId, seatIds, bookingId, clock.instant());
        if (updated != seatIds.size()) {
            throw new SeatNotAvailableException(showId, seatIds);
        }
    }
}
