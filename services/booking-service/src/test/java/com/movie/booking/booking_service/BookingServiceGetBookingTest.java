package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.PaymentClient;
import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.dto.PayableBooking;
import com.movie.booking.booking_service.exception.BookingNotFoundException;
import com.movie.booking.booking_service.service.BookingService;
import com.movie.booking.booking_service.service.BookingStateService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingServiceGetBookingTest {

    private final BookingStateService bookingStateService = mock(BookingStateService.class);
    private final BookingService service = new BookingService(
            bookingStateService, mock(PaymentClient.class), mock(SeatClient.class));

    @Test
    void returns_the_booking_to_its_owner() {
        UUID bookingId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        PayableBooking booking = new PayableBooking(bookingId, owner, UUID.randomUUID(),
                new BigDecimal("500.00"), BookingStatus.PENDING, List.of("A1"));
        when(bookingStateService.findPayable(bookingId)).thenReturn(Optional.of(booking));

        assertThat(service.getOwnedBooking(bookingId, owner)).isEqualTo(booking);
    }

    @Test
    void throws_not_found_when_the_booking_does_not_exist() {
        UUID bookingId = UUID.randomUUID();
        when(bookingStateService.findPayable(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnedBooking(bookingId, UUID.randomUUID()))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void throws_not_found_when_the_caller_is_not_the_owner() {
        UUID bookingId = UUID.randomUUID();
        PayableBooking booking = new PayableBooking(bookingId, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("500.00"), BookingStatus.PENDING, List.of("A1"));
        when(bookingStateService.findPayable(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getOwnedBooking(bookingId, UUID.randomUUID()))
                .isInstanceOf(BookingNotFoundException.class);
    }
}
