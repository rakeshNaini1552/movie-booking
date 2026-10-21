package com.movie.booking.seat_service.service;

import com.movie.booking.seat_service.event.BookingCancelledEvent;
import com.movie.booking.seat_service.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatReleaseService {

    private final ShowSeatRepository showSeatRepository;
    private final Clock clock;

    @Transactional
    public int releaseCancelledBooking(BookingCancelledEvent event) {
        int released = showSeatRepository.releaseBooked(
                event.showId(), event.seatIds(), event.bookingId(), clock.instant());
        if (released == 0) {
            log.info("No BOOKED seats to release for cancelled booking {} (already released or not booked)",
                    event.bookingId());
        } else {
            log.info("Released {} seats for cancelled booking {}", released, event.bookingId());
        }
        return released;
    }
}
