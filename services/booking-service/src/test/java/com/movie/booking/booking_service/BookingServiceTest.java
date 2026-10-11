package com.movie.booking.booking_service;

import com.movie.booking.booking_service.entity.Booking;
import com.movie.booking.booking_service.repository.BookingRepository;
import com.movie.booking.booking_service.repository.BookingSeatRepository;
import com.movie.booking.booking_service.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "booking.pricing.seat-price=100.00")
@Import(TestcontainersConfiguration.class)
public class BookingServiceTest {

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingSeatRepository bookingSeatRepository;
    @Autowired
    private BookingService bookingService;


    @Test
    void createBooking_saves_a_pending_booking_with_its_seats_and_total() {
        UUID userId = UUID.randomUUID();
        UUID showId = UUID.randomUUID();

        UUID bookingId = bookingService.createBooking(
                new CreateBookingCommand(userId, showId, List.of("A1", "A2")));

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getUserId()).isEqualTo(userId);
        assertThat(booking.getShowId()).isEqualTo(showId);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("200.00");

        assertThat(bookingSeatRepository.findByIdBookingId(bookingId))
                .extracting(seat -> seat.getId().getSeatId())
                .containsExactlyInAnyOrder("A1", "A2");
    }
}
