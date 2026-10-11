package com.movie.booking.booking_service;

import com.movie.booking.booking_service.entity.Booking;
import com.movie.booking.booking_service.entity.BookingSeat;
import com.movie.booking.booking_service.entity.BookingSeatId;
import com.movie.booking.booking_service.repository.BookingRepository;
import com.movie.booking.booking_service.repository.BookingSeatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Test
    void saves_a_booking_with_its_seats_and_reads_them_back() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking(bookingId, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("500.00"), Instant.parse("2026-10-11T10:00:00Z"));

        bookingRepository.save(booking);                       // booking first: the seats point to it
        bookingSeatRepository.saveAll(List.of(
                new BookingSeat(new BookingSeatId(bookingId, "A1")),
                new BookingSeat(new BookingSeatId(bookingId, "A2"))));

        Booking found = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(found.getTotalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));

        assertThat(bookingSeatRepository.findByIdBookingId(bookingId))
                .extracting(seat -> seat.getId().getSeatId())
                .containsExactlyInAnyOrder("A1", "A2");
    }
}
