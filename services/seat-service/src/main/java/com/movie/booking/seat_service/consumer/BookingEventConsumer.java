package com.movie.booking.seat_service.consumer;

import com.movie.booking.seat_service.event.BookingCancelledEvent;
import com.movie.booking.seat_service.service.SeatReleaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private static final String BOOKING_CANCELLED = "BookingCancelled";
    private static final String EVENT_TYPE_HEADER = "eventType";

    private final SeatReleaseService seatReleaseService;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "${seat.consumer.topic}")
    public void consume(String message,
                        @Header(name = EVENT_TYPE_HEADER, required = false) byte[] eventTypeHeader) {
        String eventType = eventTypeHeader == null ? null : new String(eventTypeHeader, StandardCharsets.UTF_8);
        if (!BOOKING_CANCELLED.equals(eventType)) {
            log.debug("Ignoring booking event of type {}", eventType);
            return;
        }

        BookingCancelledEvent event = jsonMapper.readValue(message, BookingCancelledEvent.class);
        seatReleaseService.releaseCancelledBooking(event);
    }
}
