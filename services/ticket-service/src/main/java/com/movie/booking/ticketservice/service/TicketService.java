package com.movie.booking.ticketservice.service;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.entity.TicketStatus;
import com.movie.booking.ticketservice.exception.InvalidQrException;
import com.movie.booking.ticketservice.exception.TicketNotFoundException;
import com.movie.booking.ticketservice.exception.TicketNotScannableException;
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
        return saved;
    }

    public Ticket getByBookingId(UUID bookingId) {
        return ticketRepository.findByBookingId(bookingId)
                .orElseThrow(() -> TicketNotFoundException.forBooking(bookingId));
    }

    public Ticket getTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> TicketNotFoundException.forTicket(ticketId));
    }

    @Transactional
    public Ticket scan(UUID ticketId, String payload) {
        UUID payloadTicketId = qrSigner.verify(payload)
                .orElseThrow(() -> new InvalidQrException("Invalid QR payload"));
        if (!payloadTicketId.equals(ticketId)) {
            throw new InvalidQrException("QR payload does not belong to this ticket");
        }

        int updated = ticketRepository.markUsed(ticketId, TicketStatus.ISSUED, TicketStatus.USED, clock.instant());
        Ticket ticket = getTicket(ticketId);
        if (updated == 0) {
            throw new TicketNotScannableException(ticket);
        }
        return ticket;
    }
}
