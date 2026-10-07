package com.movie.booking.seat_service.repository;


import com.movie.booking.seat_service.ShowSeatId;
import com.movie.booking.seat_service.entity.ShowSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, ShowSeatId> {
    List<ShowSeat> findByIdShowId(UUID showId);

    List<ShowSeat> findByIdShowIdAndIdSeatIdIn(UUID showId, List<String> seatIds);

    // ShowSeatRepository
    @Modifying
    @Query("""
    UPDATE ShowSeat s
    SET s.status = SeatStatus.BOOKED, s.bookingId = :bookingId, s.updatedAt = :now, s.version = s.version + 1
    WHERE s.id.showId = :showId
      AND s.id.seatId IN :seatIds
      AND s.status = SeatStatus.AVAILABLE
    """)
    int bookIfAvailable(@Param("showId") UUID showId,
                        @Param("seatIds") Collection<String> seatIds,
                        @Param("bookingId") UUID bookingId,
                        @Param("now") Instant now);
}
