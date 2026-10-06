package com.movie.booking.seat_service.repository;

import com.movie.booking.seat_service.ShowSeat;
import com.movie.booking.seat_service.ShowSeatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, ShowSeatId> {
    List<ShowSeat> findByIdShowId(UUID showId);
}
