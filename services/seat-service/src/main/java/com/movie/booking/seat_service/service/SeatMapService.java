package com.movie.booking.seat_service.service;

import com.movie.booking.seat_service.SeatResponse;
import com.movie.booking.seat_service.ShowSeatId;
import com.movie.booking.seat_service.entity.ShowSeat;
import com.movie.booking.seat_service.exception.SeatNotAvailableException;
import com.movie.booking.seat_service.exception.ShowNotFoundException;
import com.movie.booking.seat_service.repository.ShowSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SeatMapService {
    private final ShowSeatRepository showSeatRepository;
    private final Clock clock;

    public SeatMapService(ShowSeatRepository repository, Clock clock) {
        this.showSeatRepository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatMap(UUID showId) {
        List<ShowSeat> seats = showSeatRepository.findByIdShowId(showId);
        if (seats.isEmpty()) {
            throw new ShowNotFoundException(showId);
        }
        return seats.stream().map(SeatResponse::from).toList();
    }

    @Transactional
    public void bookSeats(UUID showId, List<String> seatIds, UUID bookingId) {
//        List<ShowSeat> seats = showSeatRepository.findByIdShowIdAndIdSeatIdIn(showId, seatIds);
//        for (ShowSeat seat : seats) {
//            seat.book(bookingId, clock.instant());   // may throw
//        }
        // bookSeats
        int updated = showSeatRepository.bookIfAvailable(showId, seatIds, bookingId, clock.instant());
        if (updated != seatIds.size()) {
            throw new SeatNotAvailableException(showId, seatIds);
        }
    }
}
