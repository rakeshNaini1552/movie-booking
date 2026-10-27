package com.movie.booking.booking_service.event;

import com.movie.booking.booking_service.repository.OutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics {

    public OutboxMetrics(OutboxRepository outboxRepository, MeterRegistry meterRegistry) {
        Gauge.builder("outbox.unsent.rows", outboxRepository, OutboxRepository::countBySentAtIsNull)
                .register(meterRegistry);
    }
}
