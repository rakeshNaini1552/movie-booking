package com.movie.booking.ticketservice.exception;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.entity.TicketStatus;

public class TicketNotScannableException extends RuntimeException {
    public TicketNotScannableException(Ticket ticket) {
        super(describe(ticket));
    }

    private static String describe(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.USED) {
            return "Ticket " + ticket.getTicketId() + " was already used at " + ticket.getUsedAt();
        }
        return "Ticket " + ticket.getTicketId() + " is " + ticket.getStatus() + " and cannot be used";
    }
}
