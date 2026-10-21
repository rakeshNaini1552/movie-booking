package com.movie.booking.ticketservice;

import com.movie.booking.ticketservice.entity.OutboxEvent;
import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.entity.TicketStatus;
import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.repository.OutboxRepository;
import com.movie.booking.ticketservice.repository.TicketRepository;
import com.movie.booking.ticketservice.service.QrSigner;
import com.movie.booking.ticketservice.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private QrSigner qrSigner;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void clean() {
        ticketRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    @Test
    void newEventIssuesOneSignedTicket() {
        BookingConfirmedEvent event = newEvent(UUID.randomUUID());

        Ticket ticket = ticketService.issueTicket(event);

        assertThat(ticketRepository.count()).isEqualTo(1);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ISSUED);
        assertThat(ticket.getBookingId()).isEqualTo(event.bookingId());
        assertThat(ticket.getEventId()).isEqualTo(event.eventId());
        assertThat(qrSigner.verify(ticket.getQrPayload())).contains(ticket.getTicketId());
    }

    @Test
    void sameEventProcessedTwiceKeepsOneTicket() {
        BookingConfirmedEvent event = newEvent(UUID.randomUUID());

        Ticket first = ticketService.issueTicket(event);
        Ticket second = ticketService.issueTicket(event);

        assertThat(ticketRepository.count()).isEqualTo(1);
        assertThat(second.getTicketId()).isEqualTo(first.getTicketId());
    }

    @Test
    void differentBookingsGetDifferentTickets() {
        Ticket first = ticketService.issueTicket(newEvent(UUID.randomUUID()));
        Ticket second = ticketService.issueTicket(newEvent(UUID.randomUUID()));

        assertThat(ticketRepository.count()).isEqualTo(2);
        assertThat(second.getTicketId()).isNotEqualTo(first.getTicketId());
    }
    
    @Test
    void newTicketWritesOneTicketIssuedOutboxRow() {
        BookingConfirmedEvent event = newEvent(UUID.randomUUID());

        Ticket ticket = ticketService.issueTicket(event);

        List<OutboxEvent> rows = outboxRepository.findAll();
        assertThat(rows).hasSize(1);
        OutboxEvent row = rows.get(0);
        assertThat(row.getEventType()).isEqualTo("TicketIssued");
        assertThat(row.getAggregateId()).isEqualTo(event.bookingId());
        assertThat(row.getSentAt()).isNull();
        assertThat(row.getPayload()).contains(ticket.getTicketId().toString());
    }

    @Test
    void duplicateEventDoesNotWriteSecondOutboxRow() {
        BookingConfirmedEvent event = newEvent(UUID.randomUUID());

        ticketService.issueTicket(event);
        ticketService.issueTicket(event);

        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    private BookingConfirmedEvent newEvent(UUID bookingId) {
        return new BookingConfirmedEvent(
                UUID.randomUUID(),
                Instant.now(),
                bookingId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of("A1", "A2"));
    }
}
