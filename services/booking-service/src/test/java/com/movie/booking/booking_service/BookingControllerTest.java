package com.movie.booking.booking_service;

import com.movie.booking.booking_service.config.SecurityConfig;
import com.movie.booking.booking_service.controller.BookingController;
import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.dto.PayBookingCommand;
import com.movie.booking.booking_service.dto.PayOutcome;
import com.movie.booking.booking_service.dto.PayableBooking;
import com.movie.booking.booking_service.exception.*;
import com.movie.booking.booking_service.service.BookingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static com.movie.booking.booking_service.TestAuth.asUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    private final UUID owner = UUID.randomUUID();

    private static final String PAY_URL = "/api/v1/bookings/{id}/payments";
    private static final String PAY_BODY = "{\"paymentToken\":\"tok_success\"}";

    @Test
    void valid_request_returns_201_with_booking_id() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.createBooking(any(CreateBookingCommand.class)))
                .thenReturn(bookingId);

        String body = """
                {"showId":"%s","seatIds":["A1","A2"]}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/bookings").with(asUser(owner)).header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()));
    }

    @Test
    void empty_seat_ids_returns_400_and_does_not_call_service() throws Exception {
        String body = """
                {"showId":"%s","seatIds":[]}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/bookings").with(asUser(owner)).header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookingService);
    }

    @Test
    void seats_unavailable_returns_409_with_message() throws Exception {
        when(bookingService.createBooking(any(CreateBookingCommand.class)))
                .thenThrow(new SeatsUnavailableException("Seats [A1] unavailable for booking 123"));

        String body = """
            {"showId":"%s","seatIds":["A1"]}
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/bookings").with(asUser(owner)).header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Seats [A1] unavailable for booking 123"));
    }

    @Test
    void seat_service_unreachable_returns_503_with_message() throws Exception {
        when(bookingService.createBooking(any(CreateBookingCommand.class)))
                .thenThrow(new SeatServiceUnavailableException("down", new RuntimeException("timeout")));

        String body = """
            {"showId":"%s","seatIds":["A1"]}
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/bookings").with(asUser(owner)).header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Seat service is temporarily unavailable, please try again"));
    }

    @Test
    void pay_success_returns_200_with_confirmed_and_passes_path_id_key_and_token() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID key = UUID.randomUUID();
        when(bookingService.pay(any()))
                .thenReturn(new PayOutcome(BookingStatus.CONFIRMED, PaymentStatus.SUCCESS));

        mockMvc.perform(post(PAY_URL, bookingId).with(asUser(owner))
                        .header("Idempotency-Key", key.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        ArgumentCaptor<PayBookingCommand> captor = ArgumentCaptor.forClass(PayBookingCommand.class);
        verify(bookingService).pay(captor.capture());
        assertThat(captor.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(captor.getValue().idempotencyKey()).isEqualTo(key);
        assertThat(captor.getValue().paymentToken()).isEqualTo("tok_success");
        assertThat(captor.getValue().userId()).isEqualTo(owner);
    }

    @Test
    void pay_declined_returns_200_with_pending_booking_and_failed_payment() throws Exception {
        when(bookingService.pay(any()))
                .thenReturn(new PayOutcome(BookingStatus.PENDING, PaymentStatus.FAILED));

        mockMvc.perform(post(PAY_URL, UUID.randomUUID()).with(asUser(owner))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));
    }

    @Test
    void pay_without_idempotency_key_returns_400() throws Exception {
        mockMvc.perform(post(PAY_URL, UUID.randomUUID()).with(asUser(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookingService);
    }

    @Test
    void pay_with_blank_token_returns_400() throws Exception {
        mockMvc.perform(post(PAY_URL, UUID.randomUUID()).with(asUser(owner))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentToken\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookingService);
    }

    @Test
    void pay_unknown_booking_returns_404() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.pay(any())).thenThrow(new BookingNotFoundException(bookingId));

        mockMvc.perform(post(PAY_URL, bookingId).with(asUser(owner))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void pay_booking_not_pending_returns_409() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.pay(any()))
                .thenThrow(new BookingNotPayableException(bookingId, BookingStatus.CONFIRMED));

        mockMvc.perform(post(PAY_URL, bookingId).with(asUser(owner))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void pay_invalid_token_returns_400_without_echoing_the_token() throws Exception {
        when(bookingService.pay(any())).thenThrow(new InvalidPaymentTokenException("rejected"));

        mockMvc.perform(post(PAY_URL, UUID.randomUUID()).with(asUser(owner))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("tok_success"))));
    }

    @Test
    void pay_payment_service_down_returns_503() throws Exception {
        when(bookingService.pay(any()))
                .thenThrow(new PaymentServiceUnavailableException("down", new RuntimeException()));

        mockMvc.perform(post(PAY_URL, UUID.randomUUID()).with(asUser(owner))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void pay_seats_not_confirmed_after_payment_returns_409() throws Exception {
        when(bookingService.pay(any()))
                .thenThrow(new SeatConfirmationFailedException("hold expired"));

        mockMvc.perform(post(PAY_URL, UUID.randomUUID()).with(asUser(owner))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void get_booking_returns_200_with_booking_details() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID showId = UUID.randomUUID();
        when(bookingService.getOwnedBooking(bookingId, owner)).thenReturn(new PayableBooking(
                bookingId, owner, showId, new BigDecimal("500.00"), BookingStatus.CONFIRMED, List.of("A1", "A2")));

        mockMvc.perform(get("/api/v1/bookings/{id}", bookingId).with(asUser(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.showId").value(showId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(500.0))
                .andExpect(jsonPath("$.seatIds[0]").value("A1"))
                .andExpect(jsonPath("$.seatIds[1]").value("A2"))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void get_unknown_booking_returns_404() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.getOwnedBooking(bookingId, owner)).thenThrow(new BookingNotFoundException(bookingId));

        mockMvc.perform(get("/api/v1/bookings/{id}", bookingId).with(asUser(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_booking_with_a_malformed_id_returns_400() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/{id}", "not-a-uuid").with(asUser(owner)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookingService);
    }

    @Test
    void cancel_returns_200_with_cancelled_status() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.cancel(bookingId, owner)).thenReturn(BookingStatus.CANCELLED);

        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId).with(asUser(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_returns_404_for_unknown_booking() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.cancel(bookingId, owner)).thenThrow(new BookingNotFoundException(bookingId));

        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId).with(asUser(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_returns_409_when_booking_is_not_cancellable() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.cancel(bookingId, owner))
                .thenThrow(new BookingNotCancellableException(bookingId, BookingStatus.EXPIRED));

        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId).with(asUser(owner)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_without_authentication_returns_401() throws Exception {
        mockMvc.perform(post("/api/v1/bookings").header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":\"%s\",\"seatIds\":[\"A1\"]}".formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService);
    }

    @Test
    void pay_without_authentication_returns_401() throws Exception {
        mockMvc.perform(post(PAY_URL, UUID.randomUUID()).header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAY_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService);
    }

    @Test
    void get_without_authentication_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService);
    }

    @Test
    void cancel_without_authentication_returns_401() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService);
    }

    @Test
    void create_passes_the_jwt_subject_as_the_booking_user_id() throws Exception {
        when(bookingService.createBooking(any(CreateBookingCommand.class))).thenReturn(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/bookings").with(asUser(owner))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":\"%s\",\"seatIds\":[\"A1\"]}".formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateBookingCommand> captor = ArgumentCaptor.forClass(CreateBookingCommand.class);
        verify(bookingService).createBooking(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(owner);
    }
}
