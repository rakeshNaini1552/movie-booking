package com.movie.booking.booking_service;

import com.movie.booking.booking_service.controller.BookingController;
import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.excepiton.SeatServiceUnavailableException;
import com.movie.booking.booking_service.excepiton.SeatsUnavailableException;
import com.movie.booking.booking_service.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Test
    void valid_request_returns_201_with_booking_id() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.createBooking(any(CreateBookingCommand.class)))
                .thenReturn(bookingId);

        String body = """
                {"userId":"%s","showId":"%s","seatIds":["A1","A2"]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()));
    }

    @Test
    void empty_seat_ids_returns_400_and_does_not_call_service() throws Exception {
        String body = """
                {"userId":"%s","showId":"%s","seatIds":[]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/bookings")
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
            {"userId":"%s","showId":"%s","seatIds":["A1"]}
            """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/bookings")
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
            {"userId":"%s","showId":"%s","seatIds":["A1"]}
            """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Seat service is temporarily unavailable, please try again"));
    }
}
