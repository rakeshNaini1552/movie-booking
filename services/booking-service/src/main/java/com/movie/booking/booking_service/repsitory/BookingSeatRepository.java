package com.movie.booking.booking_service.repsitory;

import com.movie.booking.booking_service.entity.BookingSeat;
import com.movie.booking.booking_service.entity.BookingSeatId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, BookingSeatId> {

    List<BookingSeat> findByIdBookingId(UUID bookingId);
}
