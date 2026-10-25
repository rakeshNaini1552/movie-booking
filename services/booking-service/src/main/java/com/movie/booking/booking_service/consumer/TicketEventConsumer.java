package com.movie.booking.booking_service.consumer;

import com.movie.booking.booking_service.event.TicketScannedEvent;
import com.movie.booking.booking_service.service.BookingStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventConsumer {

    public static final String EVENT_TYPE_HEADER = "eventType";
    public static final String TICKET_SCANNED = "TicketScanned";

    private final JsonMapper jsonMapper;
    private final BookingStateService bookingStateService;

    @KafkaListener(topics = "${booking.consumer.ticket-topic}")
    public void onTicketEvent(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(EVENT_TYPE_HEADER);
        String eventType = header == null ? null : new String(header.value(), StandardCharsets.UTF_8);

        if (!TICKET_SCANNED.equals(eventType)) {
            return;
        }

        TicketScannedEvent event = jsonMapper.readValue(record.value(), TicketScannedEvent.class);
        boolean updated = bookingStateService.markTicketUsed(event.bookingId(), event.occurredAt());
        log.debug("TicketScanned for booking {}: ticket_used_at updated={}", event.bookingId(), updated);
    }
}