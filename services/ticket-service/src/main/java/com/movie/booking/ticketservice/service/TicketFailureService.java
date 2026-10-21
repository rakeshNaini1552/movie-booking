package com.movie.booking.ticketservice.service;

import com.movie.booking.ticketservice.event.TicketIssueFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketFailureService {

    private static final String TICKET_ISSUE_FAILED = "TicketIssueFailed";

    private final OutboxService outboxService;
    private final Clock clock;

    @Transactional
    public void reportIssueFailed(UUID bookingId, UUID userId, String reason) {
        TicketIssueFailedEvent event = new TicketIssueFailedEvent(
                UUID.randomUUID(), clock.instant(), bookingId, userId, reason);

        outboxService.enqueue(bookingId, event.eventId(), TICKET_ISSUE_FAILED, event);
    }
}
