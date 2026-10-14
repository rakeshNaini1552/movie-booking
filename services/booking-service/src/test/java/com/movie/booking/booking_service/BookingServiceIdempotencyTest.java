package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.service.BookingService;
import com.movie.booking.booking_service.service.BookingStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BookingServiceIdempotencyTest {

    private BookingStateService stateService;
    private SeatClient seatClient;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        stateService = mock(BookingStateService.class);
        seatClient = mock(SeatClient.class);
        bookingService = new BookingService(stateService, seatClient);
    }

    @Test
    void duplicate_key_race_returns_the_booking_that_won() {
        UUID key = UUID.randomUUID();
        UUID winnerBookingId = UUID.randomUUID();
        CreateBookingCommand command =
                new CreateBookingCommand(UUID.randomUUID(), UUID.randomUUID(), List.of("A1"), key);

        // first check: nothing yet; after the insert fails: the winner's row is visible
        when(stateService.findBookingIdByKey(key))
                .thenReturn(Optional.empty(), Optional.of(winnerBookingId));
        when(stateService.createPending(command))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        UUID result = bookingService.createBooking(command);

        assertThat(result).isEqualTo(winnerBookingId);
        verifyNoInteractions(seatClient);
        verify(stateService, never()).markFailed(any());
    }

    @Test
    void integrity_violation_that_is_not_a_duplicate_key_is_rethrown() {
        UUID key = UUID.randomUUID();
        CreateBookingCommand command =
                new CreateBookingCommand(UUID.randomUUID(), UUID.randomUUID(), List.of("A1"), key);
        DataIntegrityViolationException violation = new DataIntegrityViolationException("some other rule");

        when(stateService.findBookingIdByKey(key)).thenReturn(Optional.empty());
        when(stateService.createPending(command)).thenThrow(violation);

        assertThatThrownBy(() -> bookingService.createBooking(command))
                .isSameAs(violation);
        verifyNoInteractions(seatClient);
    }
}
