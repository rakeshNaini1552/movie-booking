package com.movie.booking.booking_service;

import com.movie.booking.booking_service.entity.OutboxEvent;
import com.movie.booking.booking_service.event.BookingConfirmedEvent;
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
class BookingStateOutboxTest {

    @Autowired BookingStateService bookingStateService;
    @Autowired OutboxRepository outboxRepository;
    @Autowired
    JsonMapper jsonMapper;
    @Autowired
    JdbcTemplate jdbc;
    @MockitoSpyBean
    OutboxService outboxService;

    @BeforeEach
    void cleanUp() {
        outboxRepository.deleteAll();
        jdbc.update("DELETE FROM booking_seat");
        jdbc.update("DELETE FROM booking");
    }

    @Test
    void markConfirmed_writesOneOutboxRow() {
        UUID bookingId = UUID.randomUUID();
        insertPendingBooking(bookingId, "A1", "A2");

        boolean result = bookingStateService.markConfirmed(bookingId);

        assertThat(result).isTrue();
        List<OutboxEvent> rows = outboxRepository.findAll();
        assertThat(rows).hasSize(1);
        OutboxEvent row = rows.get(0);
        assertThat(row.getEventType()).isEqualTo("BookingConfirmed");
        assertThat(row.getAggregateId()).isEqualTo(bookingId);
        assertThat(row.getSentAt()).isNull();

        BookingConfirmedEvent event = jsonMapper.readValue(row.getPayload(), BookingConfirmedEvent.class);
        assertThat(event.eventId()).isEqualTo(row.getId());
        assertThat(event.bookingId()).isEqualTo(bookingId);
        assertThat(event.seatIds()).containsExactlyInAnyOrder("A1", "A2");
    }

    @Test
    void markConfirmed_onExpiredBooking_returnsFalseAndWritesNoRow() {
        UUID bookingId = UUID.randomUUID();
        insertPendingBooking(bookingId, "A1", "A2");
        jdbc.update("UPDATE booking SET status = 'EXPIRED' WHERE booking_id = ?", bookingId);

        boolean result = bookingStateService.markConfirmed(bookingId);

        assertThat(result).isFalse();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void markConfirmed_whenOutboxWriteFails_rollsBackStatusChange() {
        UUID bookingId = UUID.randomUUID();
        insertPendingBooking(bookingId, "A1", "A2");
        doThrow(new RuntimeException("outbox down"))
                .when(outboxService).enqueue(any(), any(), any(), any());

        assertThatThrownBy(() -> bookingStateService.markConfirmed(bookingId))
                .isInstanceOf(RuntimeException.class);

        String status = jdbc.queryForObject(
                "SELECT status FROM booking WHERE booking_id = ?", String.class, bookingId);
        assertThat(status).isEqualTo("PENDING");
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    private void insertPendingBooking(UUID bookingId, String... seatIds) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO booking (booking_id, user_id, show_id, status, total_amount,
                                     created_at, updated_at, idempotency_key)
                VALUES (?, ?, ?, 'PENDING', 200.00, ?, ?, ?)
                """,
                bookingId, UUID.randomUUID(), UUID.randomUUID(), now, now, UUID.randomUUID());
        for (String seatId : seatIds) {
            jdbc.update("INSERT INTO booking_seat (booking_id, seat_id) VALUES (?, ?)", bookingId, seatId);
        }
    }

}