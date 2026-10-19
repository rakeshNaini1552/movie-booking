package com.movie.booking.booking_service.event;

import com.movie.booking.booking_service.entity.OutboxEvent;
import com.movie.booking.booking_service.repository.OutboxRepository;
import com.movie.booking.booking_service.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.outbox.publisher.enabled", havingValue = "true")
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final OutboxService outboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${booking.outbox.poll-interval-ms}")
    public void publishPending() {
        for (OutboxEvent row : outboxRepository.findTop100BySentAtIsNullOrderByCreatedAtAsc()) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        row.getTopic(), row.getAggregateId().toString(), row.getPayload());
                record.headers().add("eventType", row.getEventType().getBytes(StandardCharsets.UTF_8));
                kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
                outboxService.markSent(row.getId());
            } catch (Exception e) {
                log.warn("Could not publish outbox event {}: {}", row.getId(), e.getMessage());
                break;
            }
        }
    }
}
