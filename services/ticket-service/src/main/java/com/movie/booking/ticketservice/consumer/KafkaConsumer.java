package com.movie.booking.ticketservice.consumer;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {
    private final TicketService ticketService;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "${ticket.consumer.topic}")
    public void consumeBookingConfirmedEvent(String message) {

        BookingConfirmedEvent event = jsonMapper.readValue(message, BookingConfirmedEvent.class);

        try {
            Ticket ticket = ticketService.issueTicket(event);
            log.info("Ticket {} issued for booking {}", ticket.getTicketId(), event.bookingId());
        } catch (DataIntegrityViolationException e) {
            log.info("Ticket already exists for booking {}, skipping duplicate", event.bookingId());
        }
    }
}
