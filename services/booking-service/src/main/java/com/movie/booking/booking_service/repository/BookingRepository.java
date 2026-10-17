package com.movie.booking.booking_service.repository;

import com.movie.booking.booking_service.BookingStatus;
import com.movie.booking.booking_service.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByIdempotencyKey(UUID idempotencyKey);

    List<Booking> findTop100ByStatusAndCreatedAtBefore(BookingStatus status, Instant cutoff);

    @Modifying
    @Query("update Booking b set b.status = :newStatus, b.updatedAt = :now "
            + "where b.bookingId = :id and b.status = :expected")
    int transition(@Param("id") UUID id,
                   @Param("expected") BookingStatus expected,
                   @Param("newStatus") BookingStatus newStatus,
                   @Param("now") Instant now);
}
