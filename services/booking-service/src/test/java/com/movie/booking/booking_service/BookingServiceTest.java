package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.entity.Booking;
import com.movie.booking.booking_service.exception.SeatServiceUnavailableException;
import com.movie.booking.booking_service.exception.SeatsUnavailableException;
import com.movie.booking.booking_service.repository.BookingRepository;
import com.movie.booking.booking_service.repository.BookingSeatRepository;
import com.movie.booking.booking_service.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "booking.pricing.seat-price=100.00")
@Import(TestcontainersConfiguration.class)
public class BookingServiceTest {

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingSeatRepository bookingSeatRepository;
    @Autowired
    private BookingService bookingService;

    @MockitoBean
    private SeatClient seatClient;

    @Test
    void createBooking_saves_a_pending_booking_with_its_seats_and_total() {
        UUID userId = UUID.randomUUID();
        UUID showId = UUID.randomUUID();

        UUID bookingId = bookingService.createBooking(
                new CreateBookingCommand(userId, showId, List.of("A1", "A2"), UUID.randomUUID()));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getUserId()).isEqualTo(userId);
        assertThat(booking.getShowId()).isEqualTo(showId);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("200.00");

        assertThat(bookingSeatRepository.findByIdBookingId(bookingId))
                .extracting(seat -> seat.getId().getSeatId())
                .containsExactlyInAnyOrder("A1", "A2");
    }

    @Test
    void hold_succeeds_booking_stays_pending() {
        UUID showId = UUID.randomUUID();

        UUID bookingId = bookingService.createBooking(
                new CreateBookingCommand(UUID.randomUUID(), showId, List.of("A1"), UUID.randomUUID()));

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(seatClient).holdSeats(eq(showId), eq(List.of("A1")), eq(bookingId));
    }

    @Test
    void hold_fails_booking_is_marked_failed_and_exception_propagates() {
        UUID showId = UUID.randomUUID();
        CreateBookingCommand command =
                new CreateBookingCommand(UUID.randomUUID(), showId, List.of("A1"), UUID.randomUUID());
        doThrow(new SeatsUnavailableException("taken"))
                .when(seatClient).holdSeats(any(), any(), any());

        assertThatThrownBy(() -> bookingService.createBooking(command))
                .isInstanceOf(SeatsUnavailableException.class);

        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getShowId().equals(showId))
                .findFirst()
                .orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.FAILED);
    }

    @Test
    void seat_service_unreachable_booking_is_marked_failed_and_exception_propagates() {
        UUID showId = UUID.randomUUID();
        CreateBookingCommand command =
                new CreateBookingCommand(UUID.randomUUID(), showId, List.of("A1"), UUID.randomUUID());
        doThrow(new SeatServiceUnavailableException("down", new RuntimeException("timeout")))
                .when(seatClient).holdSeats(any(), any(), any());

        assertThatThrownBy(() -> bookingService.createBooking(command))
                .isInstanceOf(SeatServiceUnavailableException.class);

        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getShowId().equals(showId))
                .findFirst()
                .orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.FAILED);
    }

    @Test
    void same_key_twice_returns_same_booking_and_holds_seats_once() {
        UUID showId = UUID.randomUUID();
        UUID key = UUID.randomUUID();
        CreateBookingCommand command =
                new CreateBookingCommand(UUID.randomUUID(), showId, List.of("A1"), key);

        UUID first = bookingService.createBooking(command);
        UUID second = bookingService.createBooking(command);

        assertThat(second).isEqualTo(first);

        long bookingsForShow = bookingRepository.findAll().stream()
                .filter(b -> b.getShowId().equals(showId))
                .count();
        assertThat(bookingsForShow).isEqualTo(1);

        verify(seatClient, times(1)).holdSeats(any(), any(), any());
    }

    @Test
    void different_keys_create_different_bookings() {
        UUID showId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UUID first = bookingService.createBooking(
                new CreateBookingCommand(userId, showId, List.of("A1"), UUID.randomUUID()));
        UUID second = bookingService.createBooking(
                new CreateBookingCommand(userId, showId, List.of("A2"), UUID.randomUUID()));

        assertThat(second).isNotEqualTo(first);

        long bookingsForShow = bookingRepository.findAll().stream()
                .filter(b -> b.getShowId().equals(showId))
                .count();
        assertThat(bookingsForShow).isEqualTo(2);
    }
}