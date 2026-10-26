package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.repository.OutboxRepository;
import com.movie.booking.booking_service.service.BookingExpiryService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.movie.booking.booking_service.TestAuth.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Saga check for "payment-service dies mid-flow": the booking must stay PENDING with nothing
 * written to the outbox and no seat confirmed, and the expiry job must later clean it up.
 * Uses the real PaymentServiceClient against a port that refuses connections, or a server that hangs.
 */
@SpringBootTest(properties = {
        "payment-service.read-timeout-ms=300",
        "booking.expiry.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SagaPaymentUnavailableTest {

    private static final int PAYMENT_PORT = freePort();

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired OutboxRepository outboxRepository;
    @Autowired BookingExpiryService bookingExpiryService;
    @MockitoBean SeatClient seatClient;

    private final UUID owner = UUID.randomUUID();

    @DynamicPropertySource
    static void paymentServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("payment-service.base-url", () -> "http://localhost:" + PAYMENT_PORT);
    }

    @BeforeEach
    void cleanUp() {
        reset(seatClient);
        outboxRepository.deleteAll();
        jdbc.update("DELETE FROM booking_seat");
        jdbc.update("DELETE FROM booking");
    }

    @Test
    void connectionRefusedAtPaymentCallReturns503AndLeavesBookingPendingThenExpiryExpiresIt() throws Exception {
        UUID bookingId = insertPendingBooking("A1", "A2");

        pay(bookingId);

        assertNothingConfirmedOrPublished(bookingId);
        assertExpiryExpiresIt(bookingId);
    }

    @Test
    void paymentServiceTimeoutReturns503AndLeavesBookingPendingThenExpiryExpiresIt() throws Exception {
        UUID bookingId = insertPendingBooking("A1", "A2");
        HttpServer hangingPaymentService = hangingServer();
        try {
            pay(bookingId);
        } finally {
            hangingPaymentService.stop(0);
        }

        assertNothingConfirmedOrPublished(bookingId);
        assertExpiryExpiresIt(bookingId);
    }

    private void pay(UUID bookingId) throws Exception {
        mockMvc.perform(post("/api/v1/bookings/{id}/payments", bookingId).with(asUser(owner))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentToken\":\"tok_success\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    private void assertNothingConfirmedOrPublished(UUID bookingId) {
        assertThat(statusOf(bookingId)).isEqualTo("PENDING");
        assertThat(outboxRepository.count()).isZero();
        verifyNoInteractions(seatClient);
    }

    private void assertExpiryExpiresIt(UUID bookingId) {
        jdbc.update("UPDATE booking SET created_at = ? WHERE booking_id = ?",
                Timestamp.from(Instant.now().minus(6, ChronoUnit.MINUTES)), bookingId);

        int expired = bookingExpiryService.expireStaleBookings();

        assertThat(expired).isEqualTo(1);
        assertThat(statusOf(bookingId)).isEqualTo("EXPIRED");
        assertThat(outboxRepository.count()).isZero();
    }

    private HttpServer hangingServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PAYMENT_PORT), 0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
        return server;
    }

    private String statusOf(UUID bookingId) {
        return jdbc.queryForObject("SELECT status FROM booking WHERE booking_id = ?", String.class, bookingId);
    }

    private UUID insertPendingBooking(String... seatIds) {
        UUID bookingId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO booking (booking_id, user_id, show_id, status, total_amount,
                                     created_at, updated_at, idempotency_key)
                VALUES (?, ?, ?, 'PENDING', 500.00, ?, ?, ?)
                """,
                bookingId, owner, UUID.randomUUID(), now, now, UUID.randomUUID());
        for (String seatId : seatIds) {
            jdbc.update("INSERT INTO booking_seat (booking_id, seat_id) VALUES (?, ?)", bookingId, seatId);
        }
        return bookingId;
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
