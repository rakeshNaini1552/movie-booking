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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
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
        boolean cancelled = bookingRepository.transitionIfTicketUnused(
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
                    return new PayableBooking(booking.getBookingId(), booking.getUserId(), booking.getShowId(),
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
    public boolean markTicketUsed(UUID bookingId, Instant usedAt) {
        return bookingRepository.markTicketUsed(
                bookingId,usedAt, Instant.now()) == 1;
    }

    @Transactional
    public void markFailedAndRequestRefund(UUID bookingId) {
        bookingRepository.transition(
                bookingId, BookingStatus.PENDING, BookingStatus.FAILED, clock.instant());

        publishEventByBookingIdHelper(bookingId);
    }

    // The customer paid (CONFIRMED) but ticket-service gave up issuing a ticket. Moves the booking to
    // FAILED and, in the same transaction, enqueues BookingCancelled (seats and ticket records) and
    // RefundRequested (payment). The conditional update makes a redelivery a no-op: once the booking
    // is FAILED, nothing matches and nothing is enqueued. A booking whose ticket was already scanned is
    // left alone: the customer got in, so there is nothing to refund.
    @Transactional
    public boolean compensateTicketIssueFailed(UUID bookingId) {
        int updated = bookingRepository.transitionIfTicketUnused(
                bookingId, BookingStatus.CONFIRMED, BookingStatus.FAILED, clock.instant());
        if (updated == 1) {
            publishEventByBookingIdHelper(bookingId);
            return true;
        }

        Optional<Booking> booking = bookingRepository.findById(bookingId);
        if (booking.isEmpty()) {
            log.warn("TicketIssueFailed for unknown booking {}, ignoring", bookingId);
        } else if (booking.get().getStatus() == BookingStatus.CONFIRMED) {
            log.warn("TicketIssueFailed for booking {} whose ticket was already used, not compensating", bookingId);
        } else if (booking.get().getStatus() == BookingStatus.FAILED
                || booking.get().getStatus() == BookingStatus.CANCELLED) {
            log.info("TicketIssueFailed for booking {} already {}, nothing to do", bookingId, booking.get().getStatus());
        } else {
            log.warn("TicketIssueFailed for booking {} in status {}, expected CONFIRMED, ignoring",
                    bookingId, booking.get().getStatus());
        }
        return false;
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
