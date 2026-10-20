package com.movie.booking.ticketservice;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.entity.TicketStatus;
import com.movie.booking.ticketservice.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void clean() {
        ticketRepository.deleteAll();
    }

    @Test
    void savesTicketAndFindsItByBookingId() {
        UUID bookingId = UUID.randomUUID();
        ticketRepository.saveAndFlush(newTicket(bookingId));

        Ticket found = ticketRepository.findByBookingId(bookingId).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(TicketStatus.ISSUED);
        assertThat(found.getUsedAt()).isNull();
        assertThat(found.getQrPayload()).isEqualTo("qr-" + bookingId);
    }

    @Test
    void secondTicketForSameBookingIsRejectedByUniqueConstraint() {
        UUID bookingId = UUID.randomUUID();
        ticketRepository.saveAndFlush(newTicket(bookingId));

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(newTicket(bookingId)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    void findByBookingIdReturnsEmptyWhenNoTicketExists() {
        assertThat(ticketRepository.findByBookingId(UUID.randomUUID())).isEmpty();
    }

    private Ticket newTicket(UUID bookingId) {
        return new Ticket(UUID.randomUUID(), bookingId, UUID.randomUUID(),
                "qr-" + bookingId, Instant.now());
    }
}
