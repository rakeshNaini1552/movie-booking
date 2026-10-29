package com.movie.booking.booking_service.service;

import com.movie.booking.booking_service.BookingStatus;
import com.movie.booking.booking_service.PaymentStatus;
import com.movie.booking.booking_service.client.PaymentClient;
import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.dto.*;
import com.movie.booking.booking_service.exception.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class BookingService {

    private final BookingStateService bookingStateService;
    private final PaymentClient paymentClient;
    private final SeatClient seatClient;
    private final Counter confirmedCounter;
    private final Counter seatConfirmFailedCounter;
    private final Counter notPendingCounter;
    private final Counter markConfirmedFailedCounter;

    public BookingService(BookingStateService bookingStateService, PaymentClient paymentClient,
                          SeatClient seatClient, MeterRegistry meterRegistry) {
        this.bookingStateService = bookingStateService;
        this.paymentClient = paymentClient;
        this.seatClient = seatClient;
        this.confirmedCounter = meterRegistry.counter("bookings.confirmed");
        this.seatConfirmFailedCounter = meterRegistry.counter("saga.compensations", "reason", "seat_confirm_failed");
        this.notPendingCounter = meterRegistry.counter("saga.compensations", "reason", "booking_not_pending");
        this.markConfirmedFailedCounter = meterRegistry.counter("saga.compensations", "reason", "mark_confirmed_failed");
    }

    public UUID createBooking(CreateBookingCommand command) {
        Optional<UUID> existing = bookingStateService.findBookingIdByKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        UUID bookingId;
        try {
            bookingId = bookingStateService.createPending(command);
        } catch (DataIntegrityViolationException e) {
            return bookingStateService.findBookingIdByKey(command.idempotencyKey())
                    .orElseThrow(() -> e);
        }

        try {
            seatClient.holdSeats(command.showId(), command.seatIds(), bookingId);
        } catch (SeatsUnavailableException | SeatServiceUnavailableException e) {
            log.warn("Seat hold failed for booking {}: {}", bookingId, e.getMessage());
            bookingStateService.markFailed(bookingId);
            throw e;
        }
        return bookingId;
    }

    public PayOutcome pay(PayBookingCommand command) {
        PayableBooking booking = getOwnedBooking(command.bookingId(), command.userId());

        if (booking.status() != BookingStatus.PENDING) {
            throw new BookingNotPayableException(booking.bookingId(), booking.status());
        }

        PaymentResult result = paymentClient.pay(
                booking.bookingId(),
                booking.totalAmount(),
                command.paymentToken(),
                command.idempotencyKey());

        if (result.status() == PaymentStatus.FAILED) {
            return new PayOutcome(BookingStatus.PENDING, PaymentStatus.FAILED);
        }

        try {
            seatClient.confirmSeats(booking.showId(), booking.seatIds(), booking.bookingId());
        } catch (RuntimeException e) {
            log.error("PAID but seats not confirmed for booking {} (payment {}): {}",
                    booking.bookingId(), result.paymentId(), e.getMessage());
            return failAndRequestRefund(booking.bookingId(), result.paymentId(), seatConfirmFailedCounter);
        }

        boolean confirmed;
        try {
            confirmed = bookingStateService.markConfirmed(booking.bookingId());
        } catch (RuntimeException e) {
            log.error("PAID and seats confirmed, but booking {} could not be marked CONFIRMED (payment {}): {}",
                    booking.bookingId(), result.paymentId(), e.getMessage());
            return failAndRequestRefund(booking.bookingId(), result.paymentId(), markConfirmedFailedCounter);
        }
        if (!confirmed) {
            log.error("PAID and seats confirmed, but booking {} is no longer PENDING (payment {})",
                    booking.bookingId(), result.paymentId());
            return failAndRequestRefund(booking.bookingId(), result.paymentId(), notPendingCounter);
        }
        confirmedCounter.increment();
        return new PayOutcome(BookingStatus.CONFIRMED, PaymentStatus.SUCCESS);
    }

    private PayOutcome failAndRequestRefund(UUID bookingId, UUID paymentId, Counter compensationCounter) {
        try {
            bookingStateService.markFailedAndRequestRefund(bookingId);
        } catch (RuntimeException e) {
            log.error("PAID but the refund request could not be recorded for booking {} (payment {}): {}",
                    bookingId, paymentId, e.getMessage());
            throw e;
        }
        compensationCounter.increment();
        return new PayOutcome(BookingStatus.FAILED, PaymentStatus.SUCCESS);
    }

    public BookingStatus cancel(UUID bookingId, UUID userId) {
        PayableBooking booking = getOwnedBooking(bookingId, userId);

        boolean cancelled = switch (booking.status()) {
            case CANCELLED -> true;
            case PENDING -> cancelPending(booking);
            case CONFIRMED -> bookingStateService.markCancelledFromConfirmed(bookingId);
            case FAILED, EXPIRED -> false;
        };
        if (cancelled) {
            return BookingStatus.CANCELLED;
        }

        BookingStatus current = getBooking(bookingId).status();
        if (current == BookingStatus.CANCELLED) {
            return current;
        }
        if (current == BookingStatus.CONFIRMED) {
            throw new TicketAlreadyUsedException(bookingId);
        }
        throw new BookingNotCancellableException(bookingId, current);
    }

    private boolean cancelPending(PayableBooking booking) {
        if (!bookingStateService.markCancelledFromPending(booking.bookingId())) {
            return false;
        }
        try {
            seatClient.releaseSeats(booking.showId(), booking.seatIds(), booking.bookingId());
        } catch (SeatServiceUnavailableException e) {
            // The booking is already CANCELLED; the Redis hold expires on its own TTL.
            log.warn("Booking {} cancelled but seat release failed: {}", booking.bookingId(), e.getMessage());
        }
        return true;
    }

    public PayableBooking getOwnedBooking(UUID bookingId, UUID userId) {
        PayableBooking booking = getBooking(bookingId);
        if (!booking.userId().equals(userId)) {
            throw new BookingNotFoundException(bookingId);
        }
        return booking;
    }

    private PayableBooking getBooking(UUID bookingId) {
        return bookingStateService.findPayable(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }
}
