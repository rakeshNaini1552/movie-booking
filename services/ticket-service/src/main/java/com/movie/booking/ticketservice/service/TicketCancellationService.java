package com.movie.booking.ticketservice.service;

import com.movie.booking.ticketservice.entity.CancelledBooking;
import com.movie.booking.ticketservice.entity.TicketStatus;
import com.movie.booking.ticketservice.event.BookingCancelledEvent;
import com.movie.booking.ticketservice.repository.CancelledBookingRepository;
import com.movie.booking.ticketservice.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCancellationService {

    private final TicketRepository ticketRepository;
    private final CancelledBookingRepository cancelledBookingRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public boolean isCancelled(UUID bookingId) {
        return cancelledBookingRepository.existsById(bookingId);
    }

    @Transactional
    public void cancelBooking(BookingCancelledEvent event) {
        UUID bookingId = event.bookingId();
        if (!cancelledBookingRepository.existsById(bookingId)) {
            cancelledBookingRepository.saveAndFlush(new CancelledBooking(bookingId, clock.instant()));
        }

        int voided = ticketRepository.voidIfIssued(bookingId, TicketStatus.ISSUED, TicketStatus.VOID);
        if (voided == 1) {
            log.info("Voided ticket for cancelled booking {}", bookingId);
            return;
        }
        ticketRepository.findByBookingId(bookingId).ifPresentOrElse(
                ticket -> log.warn("Ticket {} for cancelled booking {} is {}, not voiding",
                        ticket.getTicketId(), bookingId, ticket.getStatus()),
                () -> log.info("No ticket yet for cancelled booking {}, recorded cancellation", bookingId));
    }
}
