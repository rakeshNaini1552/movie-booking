package com.movie.booking.booking_service.repository;

import com.movie.booking.booking_service.entity.Booking;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByIdempotencyKey(UUID idempotencyKey);
}
