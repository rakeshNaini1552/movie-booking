package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.entity.OutboxEvent;
import com.movie.booking.booking_service.event.BookingCancelledEvent;
import com.movie.booking.booking_service.event.RefundRequestedEvent;
import com.movie.booking.booking_service.exception.BookingNotCancellableException;
import com.movie.booking.booking_service.exception.BookingNotFoundException;
import com.movie.booking.booking_service.exception.SeatServiceUnavailableException;
import com.movie.booking.booking_service.repository.OutboxRepository;
import com.movie.booking.booking_service.service.BookingService;
import com.movie.booking.booking_service.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingCancelTest {

    @Autowired BookingService bookingService;
    @Autowired OutboxRepository outboxRepository;
    @Autowired JsonMapper jsonMapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean SeatClient seatClient;
    @MockitoSpyBean OutboxService outboxService;

    @BeforeEach
    void cleanUp() {
        reset(seatClient, outboxService);
        outboxRepository.deleteAll();
        jdbc.update("DELETE FROM booking_seat");
        jdbc.update("DELETE FROM booking");
    }

    @Test
    void cancellingPendingBookingReleasesSeatsAndWritesNoEvents() {
        UUID bookingId = insertBooking("PENDING", "A1", "A2");

        BookingStatus result = bookingService.cancel(bookingId);

        assertThat(result).isEqualTo(BookingStatus.CANCELLED);
        assertThat(statusOf(bookingId)).isEqualTo("CANCELLED");
        verify(seatClient).releaseSeats(any(), any(), org.mockito.ArgumentMatchers.eq(bookingId));
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void cancellingPendingBookingSucceedsEvenWhenSeatReleaseFails() {
        UUID bookingId = insertBooking("PENDING", "A1");
        doThrow(new SeatServiceUnavailableException("down", new RuntimeException()))
                .when(seatClient).releaseSeats(any(), any(), any());

        assertThat(bookingService.cancel(bookingId)).isEqualTo(BookingStatus.CANCELLED);

        assertThat(statusOf(bookingId)).isEqualTo("CANCELLED");
    }

    @Test
    void cancellingConfirmedBookingWritesBookingCancelledAndRefundRequested() {
        UUID bookingId = insertBooking("CONFIRMED", "A1", "A2");

        assertThat(bookingService.cancel(bookingId)).isEqualTo(BookingStatus.CANCELLED);

        assertThat(statusOf(bookingId)).isEqualTo("CANCELLED");
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
        assertThat(cancelled.showId()).isNotNull();

        RefundRequestedEvent refund = jsonMapper.readValue(payload(rows, "RefundRequested"), RefundRequestedEvent.class);
        assertThat(refund.bookingId()).isEqualTo(bookingId);
        assertThat(refund.amount()).isEqualByComparingTo("200.00");
        verify(seatClient, never()).releaseSeats(any(), any(), any());
    }

    @Test
    void cancellingTwiceIsIdempotentAndWritesEventsOnce() {
        UUID bookingId = insertBooking("CONFIRMED", "A1");

        bookingService.cancel(bookingId);
        BookingStatus second = bookingService.cancel(bookingId);

        assertThat(second).isEqualTo(BookingStatus.CANCELLED);
        assertThat(outboxRepository.count()).isEqualTo(2);
    }

    @Test
    void failedAndExpiredBookingsCannotBeCancelled() {
        UUID failed = insertBooking("FAILED", "A1");
        UUID expired = insertBooking("EXPIRED", "A2");

        assertThatThrownBy(() -> bookingService.cancel(failed))
                .isInstanceOf(BookingNotCancellableException.class);
        assertThatThrownBy(() -> bookingService.cancel(expired))
                .isInstanceOf(BookingNotCancellableException.class);

        assertThat(statusOf(failed)).isEqualTo("FAILED");
        assertThat(statusOf(expired)).isEqualTo("EXPIRED");
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void unknownBookingIsNotFound() {
        assertThatThrownBy(() -> bookingService.cancel(UUID.randomUUID()))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void outboxFailureRollsBackConfirmedCancellation() {
        UUID bookingId = insertBooking("CONFIRMED", "A1");
        doThrow(new RuntimeException("outbox down"))
                .when(outboxService).enqueue(any(), any(), any(), any());

        assertThatThrownBy(() -> bookingService.cancel(bookingId)).isInstanceOf(RuntimeException.class);

        assertThat(statusOf(bookingId)).isEqualTo("CONFIRMED");
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void simultaneousCancelsOfConfirmedBookingEmitEventsOnce() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", "A1");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<BookingStatus> a = pool.submit(() -> { start.await(); return bookingService.cancel(bookingId); });
            Future<BookingStatus> b = pool.submit(() -> { start.await(); return bookingService.cancel(bookingId); });
            start.countDown();

            assertThat(a.get()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(b.get()).isEqualTo(BookingStatus.CANCELLED);
        } finally {
            pool.shutdownNow();
        }
        assertThat(outboxRepository.count()).isEqualTo(2);
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
