package com.movie.booking.seat_service.service;

import com.movie.booking.seat_service.SeatResponse;
import com.movie.booking.seat_service.entity.ShowSeat;
import com.movie.booking.seat_service.exception.ShowNotFoundException;
import com.movie.booking.seat_service.repository.ShowSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SeatMapService {
    private final ShowSeatRepository showSeatRepository;

    public SeatMapService(ShowSeatRepository repository) {
        this.showSeatRepository = repository;
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatMap(UUID showId) {
        List<ShowSeat> seats = showSeatRepository.findByIdShowId(showId);
        if (seats.isEmpty()) {
            throw new ShowNotFoundException(showId);
        }
        return seats.stream().map(SeatResponse::from).toList();
    }
}
