package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.PaymentClient;
import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.dto.PaymentResult;
import com.movie.booking.booking_service.entity.OutboxEvent;
import com.movie.booking.booking_service.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.movie.booking.booking_service.TestAuth.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingOwnershipTest {

    private static final String BOOKINGS_URL = "/api/v1/bookings";
    private static final String BOOKING_URL = "/api/v1/bookings/{id}";
    private static final String PAY_URL = "/api/v1/bookings/{id}/payments";
    private static final String CANCEL_URL = "/api/v1/bookings/{id}/cancel";
    private static final String PAY_BODY = "{\"paymentToken\":\"tok_success\"}";

    @Autowired MockMvc mockMvc;
    @Autowired OutboxRepository outboxRepository;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean PaymentClient paymentClient;
    @MockitoBean SeatClient seatClient;

    private final UUID userA = UUID.randomUUID();
    private final UUID userB = UUID.randomUUID();

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
    void create_without_authorization_header_returns_401() throws Exception {
        mockMvc.perform(post(BOOKINGS_URL)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":\"%s\",\"seatIds\":[\"A1\"]}".formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());

        assertThat(countBookings()).isZero();
    }

    @Test
    void pay_without_authorization_header_returns_401() throws Exception {
        UUID bookingId = insertBooking(userA, "PENDING", "A1");

        mockMvc.perform(post(PAY_URL, bookingId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentClient, seatClient);
        assertThat(statusOf(bookingId)).isEqualTo("PENDING");
    }

    @Test
    void get_without_authorization_header_returns_401() throws Exception {
        UUID bookingId = insertBooking(userA, "PENDING", "A1");

        mockMvc.perform(get(BOOKING_URL, bookingId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancel_without_authorization_header_returns_401() throws Exception {
        UUID bookingId = insertBooking(userA, "CONFIRMED", "A1");

        mockMvc.perform(post(CANCEL_URL, bookingId))
                .andExpect(status().isUnauthorized());

        assertThat(statusOf(bookingId)).isEqualTo("CONFIRMED");
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void booking_is_stored_under_the_jwt_subject() throws Exception {
        UUID bookingId = createBookingAs(userA);

        assertThat(jdbc.queryForObject("SELECT user_id FROM booking WHERE booking_id = ?", UUID.class, bookingId))
                .isEqualTo(userA);
    }

    @Test
    void another_user_gets_404_on_get_pay_and_cancel_and_nothing_changes() throws Exception {
        UUID bookingId = createBookingAs(userA);
        Map<String, Object> before = rowOf(bookingId);

        mockMvc.perform(get(BOOKING_URL, bookingId).with(asUser(userB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(PAY_URL, bookingId).with(asUser(userB))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(CANCEL_URL, bookingId).with(asUser(userB)))
                .andExpect(status().isNotFound());

        assertThat(rowOf(bookingId)).isEqualTo(before);
        assertThat(statusOf(bookingId)).isEqualTo("PENDING");
        assertThat(outboxRepository.count()).isZero();
        verifyNoInteractions(paymentClient);
    }

    @Test
    void another_user_cannot_cancel_a_confirmed_booking_or_add_outbox_rows() throws Exception {
        UUID bookingId = insertBooking(userA, "CONFIRMED", "A1", "A2");
        Map<String, Object> before = rowOf(bookingId);

        mockMvc.perform(post(CANCEL_URL, bookingId).with(asUser(userB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BOOKING_URL, bookingId).with(asUser(userB)))
                .andExpect(status().isNotFound());

        assertThat(rowOf(bookingId)).isEqualTo(before);
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void owner_can_get_pay_and_cancel_their_booking() throws Exception {
        UUID payable = createBookingAs(userA);

        mockMvc.perform(get(BOOKING_URL, payable).with(asUser(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(payable.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.seatIds.length()").value(2));

        mockMvc.perform(post(PAY_URL, payable).with(asUser(userA))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
        assertThat(statusOf(payable)).isEqualTo("CONFIRMED");
        assertThat(outboxRepository.findAll()).extracting(OutboxEvent::getEventType)
                .containsExactly("BookingConfirmed");

        mockMvc.perform(post(CANCEL_URL, payable).with(asUser(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(statusOf(payable)).isEqualTo("CANCELLED");
        assertThat(outboxRepository.findAll()).extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("BookingConfirmed", "BookingCancelled", "RefundRequested");
    }

    @Test
    void owner_can_cancel_a_pending_booking() throws Exception {
        UUID bookingId = createBookingAs(userA);

        mockMvc.perform(post(CANCEL_URL, bookingId).with(asUser(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(statusOf(bookingId)).isEqualTo("CANCELLED");
    }

    private UUID createBookingAs(UUID userId) throws Exception {
        String body = mockMvc.perform(post(BOOKINGS_URL).with(asUser(userId))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":\"%s\",\"seatIds\":[\"A1\",\"A2\"]}".formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.replaceAll(".*\"bookingId\"\\s*:\\s*\"([^\"]+)\".*", "$1"));
    }

    private Map<String, Object> rowOf(UUID bookingId) {
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM booking WHERE booking_id = ?", bookingId);
        row.put("seats", jdbc.queryForList(
                "SELECT seat_id FROM booking_seat WHERE booking_id = ? ORDER BY seat_id", String.class, bookingId));
        return row;
    }

    private long countBookings() {
        return jdbc.queryForObject("SELECT count(*) FROM booking", Long.class);
    }

    private String statusOf(UUID bookingId) {
        return jdbc.queryForObject("SELECT status FROM booking WHERE booking_id = ?", String.class, bookingId);
    }

    private UUID insertBooking(UUID userId, String status, String... seatIds) {
        UUID bookingId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO booking (booking_id, user_id, show_id, status, total_amount,
                                     created_at, updated_at, idempotency_key)
                VALUES (?, ?, ?, ?, 200.00, ?, ?, ?)
                """,
                bookingId, userId, UUID.randomUUID(), status, now, now, UUID.randomUUID());
        for (String seatId : seatIds) {
            jdbc.update("INSERT INTO booking_seat (booking_id, seat_id) VALUES (?, ?)", bookingId, seatId);
        }
        return bookingId;
    }
}
