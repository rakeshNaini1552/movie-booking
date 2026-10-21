package com.movie.booking.ticketservice;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.entity.TicketStatus;
import com.movie.booking.ticketservice.event.BookingCancelledEvent;
import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.repository.CancelledBookingRepository;
import com.movie.booking.ticketservice.repository.OutboxRepository;
import com.movie.booking.ticketservice.repository.TicketRepository;
import com.movie.booking.ticketservice.service.QrSigner;
import com.movie.booking.ticketservice.service.TicketCancellationService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
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
class BookingCancelledConsumerTest {

    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired TicketRepository ticketRepository;
    @Autowired CancelledBookingRepository cancelledBookingRepository;
    @Autowired OutboxRepository outboxRepository;
    @Autowired QrSigner qrSigner;
    @Autowired JsonMapper jsonMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired KafkaContainer kafka;
    @MockitoSpyBean TicketCancellationService ticketCancellationService;

    @Value("${ticket.consumer.topic}") String topic;
    @Value("${ticket.consumer.dlt-topic}") String dltTopic;

    @BeforeEach
    void clean() {
        ticketRepository.deleteAll();
        cancelledBookingRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    @Test
    void bookingCancelledVoidsIssuedTicket() {
        UUID bookingId = UUID.randomUUID();
        Ticket ticket = saveTicket(bookingId);

        send("BookingCancelled", cancelled(bookingId));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(statusOf(ticket.getTicketId())).isEqualTo(TicketStatus.VOID));
        assertThat(cancelledBookingRepository.existsById(bookingId)).isTrue();
    }

    @Test
    void duplicateDeliveryKeepsOneMarkerAndVoidTicket() {
        UUID bookingId = UUID.randomUUID();
        Ticket ticket = saveTicket(bookingId);
        UUID markerBooking = UUID.randomUUID();
        saveTicket(markerBooking);

        send("BookingCancelled", cancelled(bookingId));
        send("BookingCancelled", cancelled(bookingId));
        send("BookingCancelled", cancelled(markerBooking));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(cancelledBookingRepository.existsById(markerBooking)).isTrue());
        assertThat(statusOf(ticket.getTicketId())).isEqualTo(TicketStatus.VOID);
        assertThat(cancelledBookingRepository.count()).isEqualTo(2);
    }

    @Test
    void usedTicketIsNotVoided() {
        UUID bookingId = UUID.randomUUID();
        Ticket ticket = saveTicket(bookingId);
        jdbc.update("update ticket set status = 'USED', used_at = now() where ticket_id = ?", ticket.getTicketId());
        UUID markerBooking = UUID.randomUUID();
        saveTicket(markerBooking);

        send("BookingCancelled", cancelled(bookingId));
        send("BookingCancelled", cancelled(markerBooking));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(cancelledBookingRepository.existsById(markerBooking)).isTrue());
        assertThat(statusOf(ticket.getTicketId())).isEqualTo(TicketStatus.USED);
    }

    @Test
    void cancellationBeforeConfirmationMeansNoTicketIsEverIssued() {
        UUID bookingId = UUID.randomUUID();
        UUID laterBooking = UUID.randomUUID();

        send("BookingCancelled", cancelled(bookingId));
        send("BookingConfirmed", confirmed(bookingId));
        send("BookingConfirmed", confirmed(laterBooking));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(ticketRepository.findByBookingId(laterBooking)).isPresent());
        assertThat(ticketRepository.findByBookingId(bookingId)).isEmpty();
        assertThat(outboxRepository.findAll())
                .noneMatch(row -> row.getAggregateId().equals(bookingId));
    }

    @Test
    void otherEventTypesDoNotVoidTickets() {
        UUID bookingId = UUID.randomUUID();
        Ticket ticket = saveTicket(bookingId);
        UUID markerBooking = UUID.randomUUID();
        saveTicket(markerBooking);

        send("RefundRequested", cancelled(bookingId));
        send("BookingCancelled", cancelled(markerBooking));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(cancelledBookingRepository.existsById(markerBooking)).isTrue());
        assertThat(statusOf(ticket.getTicketId())).isEqualTo(TicketStatus.ISSUED);
        assertThat(cancelledBookingRepository.existsById(bookingId)).isFalse();
    }

    @Test
    void failingCancellationIsDeadLetteredWithoutTicketIssueFailed() {
        UUID bookingId = UUID.randomUUID();
        doThrow(new IllegalStateException("db down"))
                .when(ticketCancellationService)
                .cancelBooking(argThat(e -> e != null && bookingId.equals(e.bookingId())));
        String message = cancelled(bookingId);

        send("BookingCancelled", message);

        try (KafkaConsumer<String, String> dltConsumer = dltConsumer()) {
            dltConsumer.subscribe(List.of(dltTopic));
            List<String> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                dltConsumer.poll(Duration.ofMillis(500)).forEach(r -> received.add(r.value()));
                assertThat(received).contains(message);
            });
        }
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    private void send(String eventType, String payload) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, UUID.randomUUID().toString(), payload);
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record);
    }

    private String cancelled(UUID bookingId) {
        return jsonMapper.writeValueAsString(new BookingCancelledEvent(
                UUID.randomUUID(), Instant.now(), bookingId, UUID.randomUUID(), UUID.randomUUID(), List.of("A1")));
    }

    private String confirmed(UUID bookingId) {
        return jsonMapper.writeValueAsString(new BookingConfirmedEvent(
                UUID.randomUUID(), Instant.now(), bookingId, UUID.randomUUID(), UUID.randomUUID(), List.of("A1")));
    }

    private Ticket saveTicket(UUID bookingId) {
        UUID ticketId = UUID.randomUUID();
        return ticketRepository.saveAndFlush(new Ticket(ticketId, bookingId, UUID.randomUUID(),
                qrSigner.sign(ticketId), Instant.now()));
    }

    private TicketStatus statusOf(UUID ticketId) {
        return ticketRepository.findById(ticketId).orElseThrow().getStatus();
    }

    private KafkaConsumer<String, String> dltConsumer() {
        return new KafkaConsumer<>(Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
    }
}
