package com.movie.booking.ticketservice;

import com.movie.booking.ticketservice.entity.OutboxEvent;
import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.event.TicketIssueFailedEvent;
import com.movie.booking.ticketservice.repository.OutboxRepository;
import com.movie.booking.ticketservice.repository.TicketRepository;
import com.movie.booking.ticketservice.service.TicketService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {
        "ticket.consumer.retry.initial-interval-ms=100",
        "ticket.consumer.retry.max-interval-ms=200"
})
@Import(TestcontainersConfiguration.class)
class TicketIssueFailedTest {

    private static final String TICKET_ISSUE_FAILED = "TicketIssueFailed";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private KafkaContainer kafka;

    @MockitoSpyBean
    private TicketService ticketService;

    @Value("${ticket.consumer.topic}")
    private String topic;

    @Value("${ticket.consumer.dlt-topic}")
    private String dltTopic;

    @BeforeEach
    void clean() {
        ticketRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    @Test
    void exhaustedRetriesProduceOneTicketIssueFailedRowAndOneDeadLetter() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new IllegalStateException("qr signer down"))
                .when(ticketService).issueTicket(argThat(e -> e != null && bookingId.equals(e.bookingId())));

        String message = send(bookingId, userId);

        try (KafkaConsumer<String, String> dltConsumer = dltConsumer()) {
            dltConsumer.subscribe(List.of(dltTopic));
            List<String> received = new ArrayList<>();

            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                dltConsumer.poll(Duration.ofMillis(500)).forEach(r -> received.add(r.value()));
                assertThat(received.stream().filter(message::equals)).hasSize(1);
            });
        }

        List<OutboxEvent> rows = failedRowsFor(bookingId);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getAggregateId()).isEqualTo(bookingId);

        TicketIssueFailedEvent event = jsonMapper.readValue(rows.getFirst().getPayload(), TicketIssueFailedEvent.class);
        assertThat(event.bookingId()).isEqualTo(bookingId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.reason()).contains("IllegalStateException").contains("qr signer down");
        assertThat(ticketRepository.findByBookingId(bookingId)).isEmpty();
    }

    @Test
    void malformedMessageIsDeadLetteredWithoutTicketIssueFailed() {
        String junk = "not-json-" + UUID.randomUUID();

        sendEvent("BookingConfirmed", UUID.randomUUID().toString(), junk);

        try (KafkaConsumer<String, String> dltConsumer = dltConsumer()) {
            dltConsumer.subscribe(List.of(dltTopic));
            List<String> received = new ArrayList<>();

            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                dltConsumer.poll(Duration.ofMillis(500)).forEach(r -> received.add(r.value()));
                assertThat(received).contains(junk);
            });
        }

        assertThat(outboxRepository.findAll())
                .noneMatch(row -> TICKET_ISSUE_FAILED.equals(row.getEventType()));
    }

    @Test
    void goodMessageAfterFailedOneStillIssuesTicket() {
        UUID failingBookingId = UUID.randomUUID();
        UUID goodBookingId = UUID.randomUUID();
        doThrow(new IllegalStateException("qr signer down"))
                .when(ticketService).issueTicket(argThat(e -> e != null && failingBookingId.equals(e.bookingId())));

        send(failingBookingId, UUID.randomUUID());
        send(goodBookingId, UUID.randomUUID());

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(ticketRepository.findByBookingId(goodBookingId)).isPresent();
            assertThat(failedRowsFor(failingBookingId)).hasSize(1);
        });
        assertThat(failedRowsFor(goodBookingId)).isEmpty();
    }

    private List<OutboxEvent> failedRowsFor(UUID bookingId) {
        return outboxRepository.findAll().stream()
                .filter(row -> TICKET_ISSUE_FAILED.equals(row.getEventType()))
                .filter(row -> bookingId.equals(row.getAggregateId()))
                .toList();
    }

    private String send(UUID bookingId, UUID userId) {
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                UUID.randomUUID(), Instant.now(), bookingId,
                userId, UUID.randomUUID(), List.of("A1"));
        String message = jsonMapper.writeValueAsString(event);
        sendEvent("BookingConfirmed", bookingId.toString(), message);
        return message;
    }

    private KafkaConsumer<String, String> dltConsumer() {
        return new KafkaConsumer<>(Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
    }

    private void sendEvent(String eventType, String key, String payload) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record);
    }
}
