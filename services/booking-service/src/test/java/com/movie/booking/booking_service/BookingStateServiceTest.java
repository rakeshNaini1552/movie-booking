package com.movie.booking.booking_service;

import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.entity.Booking;
import com.movie.booking.booking_service.repository.BookingRepository;
import com.movie.booking.booking_service.service.BookingStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "booking.pricing.seat-price=100.00")
@Import(TestcontainersConfiguration.class)
class BookingStateServiceTest {

    @Autowired
    private BookingStateService bookingStateService;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void markFailed_sets_status_to_FAILED() {
        UUID bookingId = bookingStateService.createPending(
                new CreateBookingCommand(UUID.randomUUID(), UUID.randomUUID(), List.of("A1", "A2"), UUID.randomUUID()));

        bookingStateService.markFailed(bookingId);

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.FAILED);
    }

    @Test
    void markFailed_throws_when_booking_does_not_exist() {
        assertThatThrownBy(() -> bookingStateService.markFailed(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }
}