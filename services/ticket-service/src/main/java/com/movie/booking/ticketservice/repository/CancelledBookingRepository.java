package com.movie.booking.ticketservice.repository;

import com.movie.booking.ticketservice.entity.CancelledBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CancelledBookingRepository extends JpaRepository<CancelledBooking, UUID> {
}
