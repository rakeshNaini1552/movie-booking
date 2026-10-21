package com.movie.booking.ticketservice.exception;

import java.util.UUID;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String message) {
        super(message);
    }

    public static TicketNotFoundException forTicket(UUID ticketId) {
        return new TicketNotFoundException("Ticket not found: " + ticketId);
    }

    public static TicketNotFoundException forBooking(UUID bookingId) {
        return new TicketNotFoundException("No ticket for booking: " + bookingId);
    }
}
