package com.movie.booking.booking_service;

import com.movie.booking.booking_service.entity.OutboxEvent;
import com.movie.booking.booking_service.event.BookingCancelledEvent;
import com.movie.booking.booking_service.event.RefundRequestedEvent;
import com.movie.booking.booking_service.repository.OutboxRepository;
import com.movie.booking.booking_service.service.BookingStateService;
import com.movie.booking.booking_service.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingStateCompensationTest {

    @Autowired BookingStateService bookingStateService;
    @Autowired OutboxRepository outboxRepository;
    @Autowired JsonMapper jsonMapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean OutboxService outboxService;

    @BeforeEach
    void cleanUp() {
        outboxRepository.deleteAll();
        jdbc.update("DELETE FROM booking_seat");
        jdbc.update("DELETE FROM booking");
    }

    @Test
    void confirmedBooking_becomesFailed_andEnqueuesExactlyTwoOutboxRows() {
        UUID bookingId = insertBooking("CONFIRMED", "A1", "A2");

        boolean result = bookingStateService.compensateTicketIssueFailed(bookingId);

        assertThat(result).isTrue();
        assertThat(statusOf(bookingId)).isEqualTo("FAILED");
        List<OutboxEvent> rows = outboxRepository.findAll();
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("BookingCancelled", "RefundRequested");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getAggregateId()).isEqualTo(bookingId);
            assertThat(row.getSentAt()).isNull();
        });

        BookingCancelledEvent cancelled = jsonMapper.readValue(payloadOf(rows, "BookingCancelled"), BookingCancelledEvent.class);
        assertThat(cancelled.bookingId()).isEqualTo(bookingId);
        assertThat(cancelled.seatIds()).containsExactlyInAnyOrder("A1", "A2");
        RefundRequestedEvent refund = jsonMapper.readValue(payloadOf(rows, "RefundRequested"), RefundRequestedEvent.class);
        assertThat(refund.bookingId()).isEqualTo(bookingId);
        assertThat(refund.amount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void replay_changesNothingAndEnqueuesNothing() {
        UUID bookingId = insertBooking("CONFIRMED", "A1");
        assertThat(bookingStateService.compensateTicketIssueFailed(bookingId)).isTrue();

        assertThat(bookingStateService.compensateTicketIssueFailed(bookingId)).isFalse();
        assertThat(bookingStateService.compensateTicketIssueFailed(bookingId)).isFalse();

        assertThat(statusOf(bookingId)).isEqualTo("FAILED");
        assertThat(outboxRepository.findAll()).hasSize(2);
    }

    @Test
    void bookingsInOtherStates_areUntouched() {
        for (String status : List.of("PENDING", "CANCELLED", "FAILED", "EXPIRED")) {
            UUID bookingId = insertBooking(status, "A1");

            assertThat(bookingStateService.compensateTicketIssueFailed(bookingId))
                    .as("status %s", status).isFalse();

            assertThat(statusOf(bookingId)).as("status %s", status).isEqualTo(status);
        }
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void confirmedBookingWhoseTicketWasScanned_isNotCompensated() {
        UUID bookingId = insertBooking("CONFIRMED", "A1");
        jdbc.update("UPDATE booking SET ticket_used_at = ? WHERE booking_id = ?",
                Timestamp.from(Instant.now()), bookingId);

        assertThat(bookingStateService.compensateTicketIssueFailed(bookingId)).isFalse();

        assertThat(statusOf(bookingId)).isEqualTo("CONFIRMED");
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void unknownBooking_isIgnored() {
        assertThat(bookingStateService.compensateTicketIssueFailed(UUID.randomUUID())).isFalse();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void whenOutboxWriteFails_theStatusChangeRollsBack() {
        UUID bookingId = insertBooking("CONFIRMED", "A1");
        doThrow(new RuntimeException("outbox down"))
                .when(outboxService).enqueue(any(), any(), any(), any());

        assertThatThrownBy(() -> bookingStateService.compensateTicketIssueFailed(bookingId))
                .isInstanceOf(RuntimeException.class);

        assertThat(statusOf(bookingId)).isEqualTo("CONFIRMED");
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    private String payloadOf(List<OutboxEvent> rows, String eventType) {
        return rows.stream().filter(r -> r.getEventType().equals(eventType)).findFirst().orElseThrow().getPayload();
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
