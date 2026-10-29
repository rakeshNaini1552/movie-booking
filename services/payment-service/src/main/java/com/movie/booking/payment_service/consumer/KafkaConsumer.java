package com.movie.booking.payment_service.consumer;

import com.movie.booking.payment_service.event.RefundRequestedEvent;
import com.movie.booking.payment_service.service.RefundService;
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
    public static final String REFUND_REQUESTED = "RefundRequested";

    private final RefundService refundService;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "${payment.consumer.topic}")
    public void consume(String message,
                        @Header(name = EVENT_TYPE_HEADER, required = false) byte[] eventTypeHeader) {
        String eventType = eventTypeHeader == null ? null : new String(eventTypeHeader, StandardCharsets.UTF_8);
        if (REFUND_REQUESTED.equals(eventType)) {
            consumeRefundRequestedEvent(message);
        } else {
            log.debug("Ignoring booking event of type {}", eventType);
        }
    }

    private void consumeRefundRequestedEvent(String message) {
        RefundRequestedEvent event = jsonMapper.readValue(message, RefundRequestedEvent.class);
        try {
            refundService.refund(event);
        } catch (DataIntegrityViolationException e) {
            log.info("Event {} already processed, skipping duplicate refund for booking {}",
                    event.eventId(), event.bookingId());
        }
    }
}
