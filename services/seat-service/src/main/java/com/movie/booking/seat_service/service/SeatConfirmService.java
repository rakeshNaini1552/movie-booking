package com.movie.booking.seat_service.service;

import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatConfirmService {

    private final HoldStore holdStore;
    private final SeatBookingService seatBookingService;

    public void confirm(UUID showId, List<String> seatIds, UUID holdId, UUID bookingId){
        if(!holdStore.isHeldBy(showId, seatIds, holdId)) {
            throw new SeatNotAvailableException(showId, seatIds);
        }

        seatBookingService.bookSeats(showId, seatIds, bookingId);

        holdStore.release(showId, seatIds, holdId);
    }
}