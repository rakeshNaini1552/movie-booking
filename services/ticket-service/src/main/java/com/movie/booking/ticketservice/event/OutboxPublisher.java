package com.movie.booking.ticketservice.event;


import com.movie.booking.ticketservice.entity.OutboxEvent;
import com.movie.booking.ticketservice.repository.OutboxRepository;
import com.movie.booking.ticketservice.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ticket.outbox.publisher.enabled", havingValue = "true")
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final OutboxService outboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${ticket.outbox.poll-interval-ms}")
    public void publishPending() {
        for (OutboxEvent row : outboxRepository.findTop100BySentAtIsNullOrderByCreatedAtAsc()) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        row.getTopic(), row.getAggregateId().toString(), row.getPayload());
                record.headers().add("eventType", row.getEventType().getBytes(StandardCharsets.UTF_8));
                SendResult<String, String> result = kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
                outboxService.markSent(row.getId());
                log.info("Published {} event {} to {} (partition {}, offset {})",
                        row.getEventType(), row.getId(), row.getTopic(),
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } catch (Exception e) {
                log.warn("Could not publish outbox event {}: {}", row.getId(), e.getMessage());
                break;
            }
        }
    }
}
