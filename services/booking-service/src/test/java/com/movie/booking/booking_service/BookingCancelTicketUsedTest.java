package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.PaymentClient;
import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.entity.OutboxEvent;
import com.movie.booking.booking_service.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingCancelTicketUsedTest {

    private static final String CANCEL_URL = "/api/v1/bookings/{id}/cancel";

    @Autowired MockMvc mockMvc;
    @Autowired OutboxRepository outboxRepository;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean PaymentClient paymentClient;
    @MockitoBean SeatClient seatClient;

    @BeforeEach
    void cleanUp() {
        outboxRepository.deleteAll();
        jdbc.update("DELETE FROM booking_seat");
        jdbc.update("DELETE FROM booking");
    }

    @Test
    void cancellingConfirmedBookingWithUsedTicketReturns409AndChangesNothing() throws Exception {
        UUID bookingId = insertConfirmedBooking(Instant.now().minusSeconds(60), "A1", "A2");

        mockMvc.perform(post(CANCEL_URL, bookingId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Booking cannot be cancelled: ticket already used"));

        assertThat(statusOf(bookingId)).isEqualTo("CONFIRMED");
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void cancellingConfirmedBookingWithUnusedTicketReturns200AndWritesBothEvents() throws Exception {
        UUID bookingId = insertConfirmedBooking(null, "A1", "A2");

        mockMvc.perform(post(CANCEL_URL, bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(statusOf(bookingId)).isEqualTo("CANCELLED");
        List<OutboxEvent> rows = outboxRepository.findAll();
        assertThat(rows).extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("BookingCancelled", "RefundRequested");
        assertThat(rows).allSatisfy(r -> assertThat(r.getAggregateId()).isEqualTo(bookingId));
    }

    @Test
    void cancellingPendingBookingReturns200() throws Exception {
        UUID bookingId = insertBooking("PENDING", "A1");

        mockMvc.perform(post(CANCEL_URL, bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(statusOf(bookingId)).isEqualTo("CANCELLED");
    }

    @Test
    void cancellingAlreadyCancelledBookingReturns200() throws Exception {
        UUID bookingId = insertBooking("CANCELLED", "A1");

        mockMvc.perform(post(CANCEL_URL, bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void cancellingFailedBookingReturns409NotCancellable() throws Exception {
        UUID bookingId = insertBooking("FAILED", "A1");

        mockMvc.perform(post(CANCEL_URL, bookingId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Booking not cancellable"));

        assertThat(statusOf(bookingId)).isEqualTo("FAILED");
    }

    @Test
    void cancellingExpiredBookingReturns409NotCancellable() throws Exception {
        UUID bookingId = insertBooking("EXPIRED", "A1");

        mockMvc.perform(post(CANCEL_URL, bookingId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Booking not cancellable"));

        assertThat(statusOf(bookingId)).isEqualTo("EXPIRED");
    }

    private String statusOf(UUID bookingId) {
        return jdbc.queryForObject("SELECT status FROM booking WHERE booking_id = ?", String.class, bookingId);
    }

    private UUID insertConfirmedBooking(Instant ticketUsedAt, String... seatIds) {
        return insertBooking("CONFIRMED", ticketUsedAt, seatIds);
    }

    private UUID insertBooking(String status, String... seatIds) {
        return insertBooking(status, null, seatIds);
    }

    private UUID insertBooking(String status, Instant ticketUsedAt, String... seatIds) {
        UUID bookingId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO booking (booking_id, user_id, show_id, status, total_amount,
                                     created_at, updated_at, idempotency_key, ticket_used_at)
                VALUES (?, ?, ?, ?, 200.00, ?, ?, ?, ?)
                """,
                bookingId, UUID.randomUUID(), UUID.randomUUID(), status, now, now, UUID.randomUUID(),
                ticketUsedAt == null ? null : Timestamp.from(ticketUsedAt));
        for (String seatId : seatIds) {
            jdbc.update("INSERT INTO booking_seat (booking_id, seat_id) VALUES (?, ?)", bookingId, seatId);
        }
        return bookingId;
    }
}
