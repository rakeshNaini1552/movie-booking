package com.movie.booking.ticketservice;

import com.movie.booking.ticketservice.entity.OutboxEvent;
import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.entity.TicketStatus;
import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.exception.InvalidQrException;
import com.movie.booking.ticketservice.exception.TicketNotScannableException;
import com.movie.booking.ticketservice.repository.OutboxRepository;
import com.movie.booking.ticketservice.repository.TicketRepository;
import com.movie.booking.ticketservice.service.QrSigner;
import com.movie.booking.ticketservice.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private JdbcTemplate jdbc;

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

    @Test
    void firstScanMarksTicketUsedAndWritesOneTicketScannedRow() {
        Ticket ticket = ticketService.issueTicket(newEvent(UUID.randomUUID()));
        outboxRepository.deleteAll();

        ticketService.scan(ticket.getTicketId(), ticket.getQrPayload());

        assertThat(ticketRepository.findById(ticket.getTicketId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.USED);
        List<OutboxEvent> rows = outboxRepository.findAll();
        assertThat(rows).hasSize(1);
        OutboxEvent row = rows.get(0);
        assertThat(row.getEventType()).isEqualTo("TicketScanned");
        assertThat(row.getAggregateId()).isEqualTo(ticket.getBookingId());
        assertThat(row.getSentAt()).isNull();
        assertThat(row.getPayload())
                .contains(ticket.getTicketId().toString())
                .contains(ticket.getBookingId().toString());
    }

    @Test
    void secondScanIsRejectedAndWritesNoExtraOutboxRow() {
        Ticket ticket = ticketService.issueTicket(newEvent(UUID.randomUUID()));
        ticketService.scan(ticket.getTicketId(), ticket.getQrPayload());
        long rowsAfterFirstScan = outboxRepository.count();

        assertThatThrownBy(() -> ticketService.scan(ticket.getTicketId(), ticket.getQrPayload()))
                .isInstanceOf(TicketNotScannableException.class);

        assertThat(outboxRepository.count()).isEqualTo(rowsAfterFirstScan);
        assertThat(outboxRepository.findAll()).extracting(OutboxEvent::getEventType)
                .filteredOn("TicketScanned"::equals).hasSize(1);
    }

    @Test
    void invalidQrPayloadIsRejectedAndWritesNoScannedRow() {
        Ticket ticket = ticketService.issueTicket(newEvent(UUID.randomUUID()));
        outboxRepository.deleteAll();

        assertThatThrownBy(() -> ticketService.scan(ticket.getTicketId(), "not-a-valid-payload"))
                .isInstanceOf(InvalidQrException.class);

        assertThat(outboxRepository.count()).isZero();
        assertThat(ticketRepository.findById(ticket.getTicketId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.ISSUED);
    }

    @Test
    void payloadOfAnotherTicketIsRejectedAndWritesNoScannedRow() {
        Ticket ticket = ticketService.issueTicket(newEvent(UUID.randomUUID()));
        Ticket other = ticketService.issueTicket(newEvent(UUID.randomUUID()));
        outboxRepository.deleteAll();

        assertThatThrownBy(() -> ticketService.scan(ticket.getTicketId(), other.getQrPayload()))
                .isInstanceOf(InvalidQrException.class);

        assertThat(outboxRepository.count()).isZero();
        assertThat(ticketRepository.findById(ticket.getTicketId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.ISSUED);
    }

    @Test
    void voidTicketCannotBeScannedAndWritesNoScannedRow() {
        Ticket ticket = ticketService.issueTicket(newEvent(UUID.randomUUID()));
        jdbc.update("UPDATE ticket SET status = 'VOID' WHERE ticket_id = ?", ticket.getTicketId());
        outboxRepository.deleteAll();

        assertThatThrownBy(() -> ticketService.scan(ticket.getTicketId(), ticket.getQrPayload()))
                .isInstanceOf(TicketNotScannableException.class);

        assertThat(outboxRepository.count()).isZero();
        assertThat(ticketRepository.findById(ticket.getTicketId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.VOID);
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
