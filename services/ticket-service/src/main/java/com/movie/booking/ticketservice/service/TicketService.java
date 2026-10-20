package com.movie.booking.ticketservice.service;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final QrSigner qrSigner;
    private final Clock clock;

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

        return ticketRepository.saveAndFlush(ticket);
    }
}
