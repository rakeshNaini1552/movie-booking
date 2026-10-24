package com.movie.booking.booking_service.service;

import com.movie.booking.booking_service.BookingStatus;
import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.dto.PayableBooking;
import com.movie.booking.booking_service.entity.Booking;
import com.movie.booking.booking_service.entity.BookingSeat;
import com.movie.booking.booking_service.entity.BookingSeatId;
import com.movie.booking.booking_service.event.BookingCancelledEvent;
import com.movie.booking.booking_service.event.BookingConfirmedEvent;
import com.movie.booking.booking_service.event.RefundRequestedEvent;
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
    private final OutboxService outboxService;

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
    public boolean markConfirmed(UUID bookingId) {
        boolean confirmed = bookingRepository.transition(
                bookingId, BookingStatus.PENDING, BookingStatus.CONFIRMED, clock.instant()) == 1;
        if (!confirmed) {
            return false;
        }
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        List<String> seatIds = bookingSeatRepository.findByIdBookingId(bookingId)
                .stream()
                .map(seat->seat.getId().getSeatId())
                .toList();

        UUID eventId = UUID.randomUUID();
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                eventId, clock.instant(), bookingId, booking.getUserId(), booking.getShowId(), seatIds);
        outboxService.enqueue(bookingId, eventId, "BookingConfirmed", event);
        return true;
    }

    @Transactional
    public boolean markCancelledFromPending(UUID bookingId) {
        return bookingRepository.transition(
                bookingId, BookingStatus.PENDING, BookingStatus.CANCELLED, clock.instant()) == 1;
    }

    @Transactional
    public boolean markCancelledFromConfirmed(UUID bookingId) {
        boolean cancelled = bookingRepository.transition(
                bookingId, BookingStatus.CONFIRMED, BookingStatus.CANCELLED, clock.instant()) == 1;
        if (!cancelled) {
            return false;
        }
        publishEventByBookingIdHelper(bookingId);
        return true;
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

    @Transactional(readOnly = true)
    public List<UUID> findStalePendingIds(Instant cutoff) {
        return bookingRepository.findTop100ByStatusAndCreatedAtBefore(BookingStatus.PENDING, cutoff)
                .stream()
                .map(Booking::getBookingId)
                .toList();
    }

    @Transactional
    public boolean markExpired(UUID bookingId) {
        return bookingRepository.transition(
                bookingId, BookingStatus.PENDING, BookingStatus.EXPIRED, clock.instant()) == 1;
    }

    @Transactional
    public void markFailedAndRequestRefund(UUID bookingId) {
        bookingRepository.transition(
                bookingId, BookingStatus.PENDING, BookingStatus.FAILED, clock.instant());

        publishEventByBookingIdHelper(bookingId);
    }

    private void publishEventByBookingIdHelper(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        List<String> seatIds = bookingSeatRepository.findByIdBookingId(bookingId)
                .stream()
                .map(seat -> seat.getId().getSeatId())
                .toList();

        UUID cancelledEventId = UUID.randomUUID();
        outboxService.enqueue(bookingId, cancelledEventId, "BookingCancelled",
                new BookingCancelledEvent(cancelledEventId, clock.instant(), bookingId,
                        booking.getUserId(), booking.getShowId(), seatIds));

        UUID refundEventId = UUID.randomUUID();
        outboxService.enqueue(bookingId, refundEventId, "RefundRequested",
                new RefundRequestedEvent(refundEventId, clock.instant(), bookingId,
                        booking.getUserId(), booking.getTotalAmount()));
    }
}
