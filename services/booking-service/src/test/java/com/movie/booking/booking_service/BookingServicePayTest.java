package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.PaymentClient;
import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.dto.PayBookingCommand;
import com.movie.booking.booking_service.dto.PayOutcome;
import com.movie.booking.booking_service.dto.PayableBooking;
import com.movie.booking.booking_service.dto.PaymentResult;
import com.movie.booking.booking_service.exception.BookingNotFoundException;
import com.movie.booking.booking_service.exception.BookingNotPayableException;
import com.movie.booking.booking_service.exception.PaymentServiceUnavailableException;
import com.movie.booking.booking_service.exception.SeatConfirmationFailedException;
import com.movie.booking.booking_service.service.BookingService;
import com.movie.booking.booking_service.service.BookingStateService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingServicePayTest {

    private final BookingStateService bookingStateService = mock(BookingStateService.class);
    private final SeatClient seatClient = mock(SeatClient.class);
    private final PaymentClient paymentClient = mock(PaymentClient.class);
    private final BookingService service =
            new BookingService(bookingStateService, paymentClient, seatClient);

    private final UUID bookingId = UUID.randomUUID();
    private final UUID showId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID key = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("500.00");
    private final List<String> seats = List.of("A1", "A2");
    private final PayBookingCommand command = new PayBookingCommand(bookingId, "tok_success", key);

    private void givenBooking(BookingStatus status) {
        when(bookingStateService.findPayable(bookingId))
                .thenReturn(Optional.of(new PayableBooking(bookingId, showId, amount, status, seats)));
    }

    @Test
    void successful_payment_confirms_seats_then_marks_confirmed() {
        givenBooking(BookingStatus.PENDING);
        when(bookingStateService.markConfirmed(bookingId)).thenReturn(true);
        when(paymentClient.pay(bookingId, amount, "tok_success", key))
                .thenReturn(new PaymentResult(paymentId, PaymentStatus.SUCCESS));

        PayOutcome result = service.pay(command);

        assertThat(result.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        InOrder order = inOrder(paymentClient, seatClient, bookingStateService);
        order.verify(paymentClient).pay(bookingId, amount, "tok_success", key);
        order.verify(seatClient).confirmSeats(showId, seats, bookingId);
        order.verify(bookingStateService).markConfirmed(bookingId);
    }

    @Test
    void declined_payment_keeps_booking_pending_and_does_not_confirm_seats() {
        givenBooking(BookingStatus.PENDING);
        when(paymentClient.pay(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(paymentId, PaymentStatus.FAILED));

        PayOutcome result = service.pay(command);

        assertThat(result.bookingStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
        verifyNoInteractions(seatClient);
        verify(bookingStateService, never()).markFailed(any());
        verify(bookingStateService, never()).markConfirmed(any());
    }

    @Test
    void retry_after_decline_with_a_new_key_and_token_can_succeed() {
        givenBooking(BookingStatus.PENDING);
        when(bookingStateService.markConfirmed(bookingId)).thenReturn(true);
        UUID secondKey = UUID.randomUUID();
        when(paymentClient.pay(bookingId, amount, "tok_decline", key))
                .thenReturn(new PaymentResult(UUID.randomUUID(), PaymentStatus.FAILED));
        when(paymentClient.pay(bookingId, amount, "tok_success", secondKey))
                .thenReturn(new PaymentResult(paymentId, PaymentStatus.SUCCESS));

        PayOutcome first = service.pay(new PayBookingCommand(bookingId, "tok_decline", key));
        PayOutcome second = service.pay(new PayBookingCommand(bookingId, "tok_success", secondKey));

        assertThat(first.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(second.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(seatClient, times(1)).confirmSeats(showId, seats, bookingId);
    }

    @Test
    void booking_no_longer_pending_after_seat_confirm_is_failed_with_refund_requested() {
        givenBooking(BookingStatus.PENDING);
        when(paymentClient.pay(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(paymentId, PaymentStatus.SUCCESS));
        when(bookingStateService.markConfirmed(bookingId)).thenReturn(false);

        PayOutcome result = service.pay(command);

        assertThat(result.bookingStatus()).isEqualTo(BookingStatus.FAILED);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(seatClient).confirmSeats(showId, seats, bookingId);
        verify(bookingStateService).markFailedAndRequestRefund(bookingId);
    }

    @Test
    void booking_that_is_not_pending_is_refused_without_charging() {
        givenBooking(BookingStatus.CONFIRMED);

        assertThatThrownBy(() -> service.pay(command))
                .isInstanceOf(BookingNotPayableException.class);

        verifyNoInteractions(paymentClient, seatClient);
    }

    @Test
    void unknown_booking_is_refused_without_charging() {
        when(bookingStateService.findPayable(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pay(command))
                .isInstanceOf(BookingNotFoundException.class);

        verifyNoInteractions(paymentClient, seatClient);
    }

    @Test
    void confirm_failure_after_payment_marks_failed_requests_refund_and_returns_failed_outcome() {
        givenBooking(BookingStatus.PENDING);
        when(paymentClient.pay(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(paymentId, PaymentStatus.SUCCESS));
        doThrow(new SeatConfirmationFailedException("hold expired"))
                .when(seatClient).confirmSeats(showId, seats, bookingId);

        PayOutcome result = service.pay(command);

        assertThat(result.bookingStatus()).isEqualTo(BookingStatus.FAILED);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(bookingStateService).markFailedAndRequestRefund(bookingId);
        verify(bookingStateService, never()).markConfirmed(any());
        verify(bookingStateService, never()).markFailed(any());
    }

    @Test
    void payment_service_down_changes_nothing_and_rethrows() {
        givenBooking(BookingStatus.PENDING);
        when(paymentClient.pay(any(), any(), any(), any()))
                .thenThrow(new PaymentServiceUnavailableException("down", new RuntimeException()));

        assertThatThrownBy(() -> service.pay(command))
                .isInstanceOf(PaymentServiceUnavailableException.class);

        verifyNoInteractions(seatClient);
        verify(bookingStateService, never()).markConfirmed(any());
        verify(bookingStateService, never()).markFailed(any());
    }
}