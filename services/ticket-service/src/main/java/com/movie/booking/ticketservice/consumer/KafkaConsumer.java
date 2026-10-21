package com.movie.booking.ticketservice.consumer;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.event.BookingCancelledEvent;
import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.service.TicketCancellationService;
import com.movie.booking.ticketservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    public static final String EVENT_TYPE_HEADER = "eventType";
    public static final String BOOKING_CONFIRMED = "BookingConfirmed";
    public static final String BOOKING_CANCELLED = "BookingCancelled";

    private final TicketService ticketService;
    private final TicketCancellationService ticketCancellationService;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "${ticket.consumer.topic}")
    public void consume(String message,
                        @Header(name = EVENT_TYPE_HEADER, required = false) byte[] eventTypeHeader) {
        String eventType = eventTypeHeader == null ? null : new String(eventTypeHeader, StandardCharsets.UTF_8);
        if (BOOKING_CONFIRMED.equals(eventType)) {
            consumeBookingConfirmedEvent(message);
        } else if (BOOKING_CANCELLED.equals(eventType)) {
            consumeBookingCancelledEvent(message);
        } else {
            log.debug("Ignoring booking event of type {}", eventType);
        }
    }

    private void consumeBookingConfirmedEvent(String message) {
        BookingConfirmedEvent event = jsonMapper.readValue(message, BookingConfirmedEvent.class);

        if (ticketCancellationService.isCancelled(event.bookingId())) {
            log.info("Booking {} was already cancelled, not issuing a ticket", event.bookingId());
            return;
        }

        try {
            Ticket ticket = ticketService.issueTicket(event);
            log.info("Ticket {} issued for booking {}", ticket.getTicketId(), event.bookingId());
        } catch (DataIntegrityViolationException e) {
            log.info("Ticket already exists for booking {}, skipping duplicate", event.bookingId());
        }
    }

    private void consumeBookingCancelledEvent(String message) {
        BookingCancelledEvent event = jsonMapper.readValue(message, BookingCancelledEvent.class);

        try {
            ticketCancellationService.cancelBooking(event);
        } catch (DataIntegrityViolationException e) {
            log.info("Cancellation for booking {} already recorded, skipping duplicate", event.bookingId());
        }
    }
}
