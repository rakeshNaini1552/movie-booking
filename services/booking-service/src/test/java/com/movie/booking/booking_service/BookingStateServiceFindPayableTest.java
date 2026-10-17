package com.movie.booking.booking_service;

import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.dto.PayableBooking;
import com.movie.booking.booking_service.service.BookingStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingStateServiceFindPayableTest {

    @Autowired
    private BookingStateService service;

    @Value("${booking.pricing.seat-price}")
    private BigDecimal seatPrice;

    @Test
    void returns_show_amount_status_and_seat_ids_of_an_existing_booking() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = service.createPending(new CreateBookingCommand(
                UUID.randomUUID(), showId, List.of("A1", "A2"), UUID.randomUUID()));

        Optional<PayableBooking> result = service.findPayable(bookingId);

        assertThat(result).isPresent();
        PayableBooking payable = result.get();
        assertThat(payable.bookingId()).isEqualTo(bookingId);
        assertThat(payable.showId()).isEqualTo(showId);
        assertThat(payable.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(payable.seatIds()).containsExactlyInAnyOrder("A1", "A2");
        assertThat(payable.totalAmount()).isEqualByComparingTo(seatPrice.multiply(BigDecimal.valueOf(2)));
    }

    @Test
    void returns_only_the_seats_of_the_requested_booking() {
        UUID first = service.createPending(new CreateBookingCommand(
                UUID.randomUUID(), UUID.randomUUID(), List.of("A1", "A2"), UUID.randomUUID()));
        UUID second = service.createPending(new CreateBookingCommand(
                UUID.randomUUID(), UUID.randomUUID(), List.of("B1"), UUID.randomUUID()));

        assertThat(service.findPayable(first).orElseThrow().seatIds())
                .containsExactlyInAnyOrder("A1", "A2");
        assertThat(service.findPayable(second).orElseThrow().seatIds())
                .containsExactly("B1");
    }

    @Test
    void unknown_booking_returns_empty() {
        assertThat(service.findPayable(UUID.randomUUID())).isEmpty();
    }
}