package com.movie.booking.seat_service.service;

import com.movie.booking.seat_service.dto.SeatResponse;
import com.movie.booking.seat_service.SeatStatus;
import com.movie.booking.seat_service.entity.ShowSeat;
import com.movie.booking.seat_service.exception.ShowNotFoundException;
import com.movie.booking.seat_service.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatMapService {
    private final ShowSeatRepository showSeatRepository;
    private final HoldStore holdStore;


    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatMap(UUID showId) {
        List<ShowSeat> seats = showSeatRepository.findByIdShowId(showId);
        if (seats.isEmpty()) {
            throw new ShowNotFoundException(showId);
        }
        List<String> availableIds = seats.stream()
                .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                .map(s -> s.getId().getSeatId())
                .toList();

        Set<String> held = new HashSet<>(holdStore.getHeldSeats(showId, availableIds));

        return seats.stream()
                .map(s -> SeatResponse.from(s, held.contains(s.getId().getSeatId())))
                .toList();    }
}
