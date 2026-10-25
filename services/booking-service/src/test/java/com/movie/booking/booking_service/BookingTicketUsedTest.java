package com.movie.booking.booking_service;

import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.repository.BookingRepository;
import com.movie.booking.booking_service.service.BookingStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingTicketUsedTest {

    @Autowired
    private BookingStateService service;

    @Autowired
    private BookingRepository bookingRepository;

    private UUID newPendingBooking() {
        return service.createPending(new CreateBookingCommand(
                UUID.randomUUID(), UUID.randomUUID(), List.of("A1"), UUID.randomUUID()));
    }

    private Instant ticketUsedAtOf(UUID bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow().getTicketUsedAt();
    }

    @Test
    void markTicketUsed_sets_ticket_used_at_when_empty() {
        UUID bookingId = newPendingBooking();
        Instant usedAt = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.MICROS);

        assertThat(service.markTicketUsed(bookingId, usedAt)).isTrue();
        assertThat(ticketUsedAtOf(bookingId)).isEqualTo(usedAt);
    }

    @Test
    void markTicketUsed_twice_returns_false_and_keeps_first_instant() {
        UUID bookingId = newPendingBooking();
        Instant first = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.MICROS);
        Instant second = first.plusSeconds(30);
        service.markTicketUsed(bookingId, first);

        assertThat(service.markTicketUsed(bookingId, second)).isFalse();
        assertThat(ticketUsedAtOf(bookingId)).isEqualTo(first);
    }

    @Test
    void markTicketUsed_for_unknown_booking_returns_false() {
        assertThat(service.markTicketUsed(UUID.randomUUID(), Instant.now())).isFalse();
    }
}
