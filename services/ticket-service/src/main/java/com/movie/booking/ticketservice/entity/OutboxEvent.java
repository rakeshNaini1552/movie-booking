package com.movie.booking.ticketservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    private UUID id;
    private UUID aggregateId;
    private String eventType;
    private String topic;
    private String payload;
    private Instant createdAt;
    private Instant sentAt;

    public OutboxEvent(UUID id, UUID aggregateId, String eventType, String topic, String payload, Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markSent(Instant now) {
        this.sentAt = now;
    }
}
