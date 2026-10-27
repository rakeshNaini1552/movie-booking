package com.movie.booking.booking_service;

import com.movie.booking.booking_service.event.OutboxMetrics;
import com.movie.booking.booking_service.repository.OutboxRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxMetricsTest {

    private final OutboxRepository outboxRepository = mock(OutboxRepository.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void gauge_reads_the_current_unsent_count_on_every_scrape() {
        when(outboxRepository.countBySentAtIsNull()).thenReturn(4L, 0L);
        new OutboxMetrics(outboxRepository, registry);

        assertThat(registry.get("outbox.unsent.rows").gauge().value()).isEqualTo(4.0);
        assertThat(registry.get("outbox.unsent.rows").gauge().value()).isEqualTo(0.0);
    }
}
