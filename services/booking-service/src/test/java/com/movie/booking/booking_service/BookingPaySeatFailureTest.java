package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.PaymentClient;
import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.dto.PayBookingCommand;
import com.movie.booking.booking_service.dto.PayOutcome;
import com.movie.booking.booking_service.dto.PaymentResult;
import com.movie.booking.booking_service.entity.OutboxEvent;
import com.movie.booking.booking_service.event.BookingCancelledEvent;
import com.movie.booking.booking_service.event.RefundRequestedEvent;
import com.movie.booking.booking_service.exception.BookingNotPayableException;
import com.movie.booking.booking_service.exception.SeatConfirmationFailedException;
import com.movie.booking.booking_service.exception.SeatServiceUnavailableException;
import com.movie.booking.booking_service.repository.OutboxRepository;
import com.movie.booking.booking_service.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingPaySeatFailureTest {

    @Autowired BookingService bookingService;
    @Autowired OutboxRepository outboxRepository;
    @Autowired JsonMapper jsonMapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean PaymentClient paymentClient;
    @MockitoBean SeatClient seatClient;

    @BeforeEach
    void cleanUp() {
        reset(paymentClient, seatClient);
        outboxRepository.deleteAll();
        jdbc.update("DELETE FROM booking_seat");
        jdbc.update("DELETE FROM booking");
        when(paymentClient.pay(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(UUID.randomUUID(), PaymentStatus.SUCCESS));
    }

    @Test
    void seatServiceUnavailableAfterPaymentFailsBookingAndRequestsRefund() {
        UUID bookingId = insertBooking("PENDING", "A1", "A2");
        doThrow(new SeatServiceUnavailableException("down", new RuntimeException()))
                .when(seatClient).confirmSeats(any(), any(), any());

        PayOutcome outcome = bookingService.pay(payCommand(bookingId));

        assertThat(outcome).isEqualTo(new PayOutcome(BookingStatus.FAILED, PaymentStatus.SUCCESS));
        assertFailedWithCancelAndRefundEvents(bookingId);
    }

    @Test
    void seatConfirmationFailureAfterPaymentFailsBookingAndRequestsRefund() {
        UUID bookingId = insertBooking("PENDING", "A1", "A2");
        doThrow(new SeatConfirmationFailedException("hold expired"))
                .when(seatClient).confirmSeats(any(), any(), any());

        PayOutcome outcome = bookingService.pay(payCommand(bookingId));

        assertThat(outcome).isEqualTo(new PayOutcome(BookingStatus.FAILED, PaymentStatus.SUCCESS));
        assertFailedWithCancelAndRefundEvents(bookingId);
    }

    @Test
    void bookingCancelledDuringPaymentStaysCancelledButStillRequestsRefund() {
        UUID bookingId = insertBooking("PENDING", "A1");
        doAnswer(invocation -> {
            jdbc.update("UPDATE booking SET status = 'CANCELLED' WHERE booking_id = ?", bookingId);
            throw new SeatConfirmationFailedException("hold released");
        }).when(seatClient).confirmSeats(any(), any(), any());

        PayOutcome outcome = bookingService.pay(payCommand(bookingId));

        assertThat(outcome).isEqualTo(new PayOutcome(BookingStatus.FAILED, PaymentStatus.SUCCESS));
        assertThat(statusOf(bookingId)).isEqualTo("CANCELLED");
        assertThat(outboxRepository.findAll()).extracting(OutboxEvent::getEventType)
                .contains("RefundRequested");
    }

    @Test
    void payingAFailedBookingAgainIsRejectedAndWritesNoMoreEvents() {
        UUID bookingId = insertBooking("PENDING", "A1");
        doThrow(new SeatConfirmationFailedException("hold expired"))
                .when(seatClient).confirmSeats(any(), any(), any());
        bookingService.pay(payCommand(bookingId));
        long rowsAfterFirstPay = outboxRepository.count();

        assertThatThrownBy(() -> bookingService.pay(payCommand(bookingId)))
                .isInstanceOf(BookingNotPayableException.class);

        assertThat(statusOf(bookingId)).isEqualTo("FAILED");
        assertThat(outboxRepository.count()).isEqualTo(rowsAfterFirstPay).isEqualTo(2);
    }

    private void assertFailedWithCancelAndRefundEvents(UUID bookingId) {
        assertThat(statusOf(bookingId)).isEqualTo("FAILED");
        List<OutboxEvent> rows = outboxRepository.findAll();
        assertThat(rows).extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("BookingCancelled", "RefundRequested");
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.getAggregateId()).isEqualTo(bookingId);
            assertThat(r.getSentAt()).isNull();
        });

        BookingCancelledEvent cancelled = jsonMapper.readValue(payload(rows, "BookingCancelled"), BookingCancelledEvent.class);
        assertThat(cancelled.bookingId()).isEqualTo(bookingId);
        assertThat(cancelled.seatIds()).containsExactlyInAnyOrder("A1", "A2");

        RefundRequestedEvent refund = jsonMapper.readValue(payload(rows, "RefundRequested"), RefundRequestedEvent.class);
        assertThat(refund.bookingId()).isEqualTo(bookingId);
        assertThat(refund.amount()).isEqualByComparingTo("200.00");
    }

    private PayBookingCommand payCommand(UUID bookingId) {
        return new PayBookingCommand(bookingId, "tok_success", UUID.randomUUID());
    }

    private String payload(List<OutboxEvent> rows, String type) {
        return rows.stream().filter(r -> r.getEventType().equals(type)).findFirst().orElseThrow().getPayload();
    }

    private String statusOf(UUID bookingId) {
        return jdbc.queryForObject("SELECT status FROM booking WHERE booking_id = ?", String.class, bookingId);
    }

    private UUID insertBooking(String status, String... seatIds) {
        UUID bookingId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO booking (booking_id, user_id, show_id, status, total_amount,
                                     created_at, updated_at, idempotency_key)
                VALUES (?, ?, ?, ?, 200.00, ?, ?, ?)
                """,
                bookingId, UUID.randomUUID(), UUID.randomUUID(), status, now, now, UUID.randomUUID());
        for (String seatId : seatIds) {
            jdbc.update("INSERT INTO booking_seat (booking_id, seat_id) VALUES (?, ?)", bookingId, seatId);
        }
        return bookingId;
    }
}
