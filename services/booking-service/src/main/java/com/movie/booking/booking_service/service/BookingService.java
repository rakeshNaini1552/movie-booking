package com.movie.booking.booking_service.service;

import com.movie.booking.booking_service.CreateBookingCommand;
import com.movie.booking.booking_service.entity.Booking;
import com.movie.booking.booking_service.entity.BookingSeat;
import com.movie.booking.booking_service.entity.BookingSeatId;
import com.movie.booking.booking_service.repository.BookingRepository;
import com.movie.booking.booking_service.repository.BookingSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final Clock clock;

    @Value("${booking.pricing.seat-price}")
    private BigDecimal seatPrice;

    @Transactional
    public UUID createBooking(CreateBookingCommand command) {
        UUID bookingId = UUID.randomUUID();
        Instant now = clock.instant();
        BigDecimal total = seatPrice.multiply( BigDecimal.valueOf(command.seatIds().size()));

        Booking booking = new Booking(bookingId, command.userId(), command.showId(), total, now);
        bookingRepository.save(booking);

        List<BookingSeat> seats = command.seatIds().stream()
                .map(seatId -> new BookingSeat(new BookingSeatId(booking.getBookingId(), seatId)))
                .toList();
        bookingSeatRepository.saveAll(seats);

        return bookingId;
    }
}
