package com.movie.booking.booking_service.exception;

import java.util.UUID;

public class TicketAlreadyUsedException extends RuntimeException {
    public TicketAlreadyUsedException(UUID bookingId) {
        super("Booking " + bookingId + " cannot be cancelled: its ticket was already used");
    }
}