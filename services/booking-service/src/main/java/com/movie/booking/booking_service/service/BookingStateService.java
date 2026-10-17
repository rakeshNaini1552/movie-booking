package com.movie.booking.booking_service.service;

import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.dto.PayableBooking;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingStateService {
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    @Value("${booking.pricing.seat-price}")
    private BigDecimal seatPrice;

    public Optional<UUID> findBookingIdByKey(UUID idempotencyKey) {
        return bookingRepository.findByIdempotencyKey(idempotencyKey)
                .map(Booking::getBookingId);
    }

    @Transactional
    public UUID createPending(CreateBookingCommand command){
        UUID bookingId = UUID.randomUUID();
        Instant now = clock.instant();
        BigDecimal total = seatPrice.multiply( BigDecimal.valueOf(command.seatIds().size()));

        Booking booking = new Booking(bookingId, command.userId(), command.showId(), total, now, command.idempotencyKey());
        bookingRepository.save(booking);

        List<BookingSeat> seats = command.seatIds().stream()
                .map(seatId -> new BookingSeat(new BookingSeatId(booking.getBookingId(), seatId)))
                .toList();
        bookingSeatRepository.saveAll(seats);

        return bookingId;
    }
    @Transactional
    public void markFailed(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found" + bookingId));
        booking.markFailed(clock.instant());
        bookingRepository.save(booking);
    }

    @Transactional
    public void markConfirmed(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found " + bookingId));
        booking.markConfirmed(clock.instant());
        bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public Optional<PayableBooking> findPayable(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .map(booking -> {
                    List<String> seatIds = bookingSeatRepository.findByIdBookingId(bookingId).stream()
                            .map(seat -> seat.getId().getSeatId())
                            .toList();
                    return new PayableBooking(booking.getBookingId(), booking.getShowId(),
                            booking.getTotalAmount(), booking.getStatus(), seatIds);
                });
    }
}
