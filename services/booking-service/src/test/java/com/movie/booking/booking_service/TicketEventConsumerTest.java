package com.movie.booking.booking_service;

import com.movie.booking.booking_service.dto.CreateBookingCommand;
import com.movie.booking.booking_service.repository.BookingRepository;
import com.movie.booking.booking_service.service.BookingStateService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TicketEventConsumerTest {

    @Autowired
    private BookingStateService service;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${booking.consumer.ticket-topic}")
    private String ticketTopic;

    private UUID newPendingBooking() {
        return service.createPending(new CreateBookingCommand(
                UUID.randomUUID(), UUID.randomUUID(), List.of("A1"), UUID.randomUUID()));
    }

    private Instant ticketUsedAtOf(UUID bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow().getTicketUsedAt();
    }

    private void publish(String eventType, UUID bookingId, Instant occurredAt) throws Exception {
        String json = """
                {"eventId":"%s","occurredAt":"%s","ticketId":"%s","bookingId":"%s"}"""
                .formatted(UUID.randomUUID(), occurredAt, UUID.randomUUID(), bookingId);
        ProducerRecord<String, String> record = new ProducerRecord<>(ticketTopic, bookingId.toString(), json);
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).get();
    }

    private Instant micros(long minusSeconds) {
        return Instant.now().minusSeconds(minusSeconds).truncatedTo(ChronoUnit.MICROS);
    }

    @Test
    void ticket_scanned_sets_ticket_used_at() throws Exception {
        UUID bookingId = newPendingBooking();
        Instant occurredAt = micros(60);

        publish("TicketScanned", bookingId, occurredAt);

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(ticketUsedAtOf(bookingId)).isEqualTo(occurredAt));
    }

    @Test
    void other_event_types_are_ignored() throws Exception {
        UUID ignoredBooking = newPendingBooking();
        UUID sentinelBooking = newPendingBooking();
        Instant sentinelAt = micros(30);

        publish("TicketIssued", ignoredBooking, micros(60));
        // Same partition key space, later offset: once the sentinel is processed the TicketIssued before it was too.
        publish("TicketScanned", sentinelBooking, sentinelAt);

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(ticketUsedAtOf(sentinelBooking)).isEqualTo(sentinelAt));
        assertThat(ticketUsedAtOf(ignoredBooking)).isNull();
    }

    @Test
    void duplicate_ticket_scanned_keeps_first_occurred_at() throws Exception {
        UUID bookingId = newPendingBooking();
        UUID sentinelBooking = newPendingBooking();
        Instant first = micros(60);
        Instant second = micros(10);
        Instant sentinelAt = micros(5);

        publish("TicketScanned", bookingId, first);
        publish("TicketScanned", bookingId, second);
        publish("TicketScanned", sentinelBooking, sentinelAt);

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(ticketUsedAtOf(sentinelBooking)).isEqualTo(sentinelAt));
        assertThat(ticketUsedAtOf(bookingId)).isEqualTo(first);
    }
}
