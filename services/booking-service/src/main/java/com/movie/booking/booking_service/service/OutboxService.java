package com.movie.booking.booking_service.service;

import com.movie.booking.booking_service.entity.OutboxEvent;
import com.movie.booking.booking_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final String topic;

    public OutboxService(OutboxRepository outboxRepository,
                         JsonMapper jsonMapper,
                         Clock clock,
                         @Value("${booking.outbox.topic}") String topic) {
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.topic = topic;
    }

    public void enqueue(UUID aggregateId, UUID eventId, String eventType, Object event) {
        String payload = jsonMapper.writeValueAsString(event);
        outboxRepository.save(new OutboxEvent(eventId, aggregateId, eventType, topic, payload, clock.instant()));
    }

    @Transactional
    public void markSent(UUID id) {
        outboxRepository.findById(id).ifPresent(row -> row.markSent(Instant.now()));
    }
}
