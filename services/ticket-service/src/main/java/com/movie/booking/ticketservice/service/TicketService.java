package com.movie.booking.ticketservice.service;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.event.TicketIssuedEvent;
import com.movie.booking.ticketservice.repository.TicketRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final String TICKET_ISSUED = "TicketIssued";

    private final TicketRepository ticketRepository;
    private final OutboxService outboxService;
    private final QrSigner qrSigner;
    private final Clock clock;

    @Transactional
    public Ticket issueTicket(BookingConfirmedEvent event) {
        return ticketRepository.findByBookingId(event.bookingId())
                .orElseGet(() -> create(event));
    }

    private Ticket create(BookingConfirmedEvent event) {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .ticketId(ticketId)
                .bookingId(event.bookingId())
                .eventId(event.eventId())
                .qrPayload(qrSigner.sign(ticketId))
                .issuedAt(clock.instant())
                .build();

        Ticket saved = ticketRepository.saveAndFlush(ticket);

        TicketIssuedEvent issued = new TicketIssuedEvent(
                UUID.randomUUID(),
                clock.instant(),
                saved.getTicketId(),
                saved.getBookingId(),
                event.userId());

        outboxService.enqueue(saved.getBookingId(), issued.eventId(), TICKET_ISSUED, issued);
        return saved;    }
}
