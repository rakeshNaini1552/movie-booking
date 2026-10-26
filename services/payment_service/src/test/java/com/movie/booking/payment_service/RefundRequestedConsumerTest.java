package com.movie.booking.payment_service;

import com.movie.booking.payment_service.entity.Payment;
import com.movie.booking.payment_service.entity.PaymentStatus;
import com.movie.booking.payment_service.event.RefundRequestedEvent;
import com.movie.booking.payment_service.repository.PaymentRepository;
import com.movie.booking.payment_service.repository.ProcessedEventRepository;
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
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "payment.consumer.retry.initial-interval-ms=100",
        "payment.consumer.retry.max-interval-ms=200"
})
@Import(TestcontainersConfiguration.class)
class RefundRequestedConsumerTest {

    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired PaymentRepository paymentRepository;
    @Autowired ProcessedEventRepository processedEventRepository;
    @Autowired JsonMapper jsonMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired KafkaContainer kafka;

    @Value("${payment.consumer.topic}") String topic;
    @Value("${payment.consumer.dlt-topic}") String dltTopic;

    @BeforeEach
    void clean() {
        paymentRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void refundRequestedMarksSuccessfulPaymentRefunded() {
        UUID bookingId = UUID.randomUUID();
        savePayment(bookingId, PaymentStatus.SUCCESS);
        RefundRequestedEvent event = refundEvent(bookingId);

        sendEvent("RefundRequested", bookingId, jsonMapper.writeValueAsString(event));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(statusOf(bookingId)).isEqualTo("REFUNDED"));
        assertThat(processedEventRepository.existsById(event.eventId())).isTrue();
    }

    @Test
    void duplicateDeliveryOfTheSameEventChangesNothing() {
        UUID bookingId = UUID.randomUUID();
        savePayment(bookingId, PaymentStatus.SUCCESS);
        RefundRequestedEvent event = refundEvent(bookingId);
        String payload = jsonMapper.writeValueAsString(event);
        sendEvent("RefundRequested", bookingId, payload);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(statusOf(bookingId)).isEqualTo("REFUNDED"));
        // Put the payment back so only the event-id guard can stop a second refund.
        jdbc.update("UPDATE payment SET status = 'SUCCESS' WHERE booking_id = ?", bookingId);

        sendEvent("RefundRequested", bookingId, payload);
        awaitConsumerCaughtUp();

        assertThat(statusOf(bookingId)).isEqualTo("SUCCESS");
        assertThat(processedEventRepository.count()).isEqualTo(2);   // the first event and the sync marker
        assertThat(deadLetters()).doesNotContain(payload);
    }

    @Test
    void refundForABookingWithoutSuccessfulPaymentIsRecordedAndIgnored() {
        UUID declined = UUID.randomUUID();
        UUID neverCharged = UUID.randomUUID();
        savePayment(declined, PaymentStatus.FAILED);
        RefundRequestedEvent declinedEvent = refundEvent(declined);
        RefundRequestedEvent unchargedEvent = refundEvent(neverCharged);

        sendEvent("RefundRequested", declined, jsonMapper.writeValueAsString(declinedEvent));
        sendEvent("RefundRequested", neverCharged, jsonMapper.writeValueAsString(unchargedEvent));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(processedEventRepository.existsById(declinedEvent.eventId())).isTrue();
            assertThat(processedEventRepository.existsById(unchargedEvent.eventId())).isTrue();
        });
        assertThat(statusOf(declined)).isEqualTo("FAILED");
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void otherEventTypesAreIgnored() {
        UUID bookingId = UUID.randomUUID();
        savePayment(bookingId, PaymentStatus.SUCCESS);
        RefundRequestedEvent lookalike = refundEvent(bookingId);

        sendEvent("BookingConfirmed", bookingId, jsonMapper.writeValueAsString(lookalike));
        awaitConsumerCaughtUp();

        assertThat(statusOf(bookingId)).isEqualTo("SUCCESS");
        assertThat(processedEventRepository.existsById(lookalike.eventId())).isFalse();
    }

    @Test
    void malformedRefundRequestedEndsUpInDeadLetterTopic() {
        String junk = "not-json-" + UUID.randomUUID();

        sendEvent("RefundRequested", UUID.randomUUID(), junk);

        try (KafkaConsumer<String, String> dltConsumer = dltConsumer()) {
            dltConsumer.subscribe(List.of(dltTopic));
            List<String> received = new ArrayList<>();

            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                dltConsumer.poll(Duration.ofMillis(500)).forEach(r -> received.add(r.value()));
                assertThat(received).contains(junk);
            });
        }
    }

    // The topic has one partition, so once a later marker event is processed every earlier record was handled.
    private void awaitConsumerCaughtUp() {
        UUID markerBooking = UUID.randomUUID();
        RefundRequestedEvent marker = refundEvent(markerBooking);
        sendEvent("RefundRequested", markerBooking, jsonMapper.writeValueAsString(marker));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(processedEventRepository.existsById(marker.eventId())).isTrue());
    }

    private List<String> deadLetters() {
        List<String> received = new ArrayList<>();
        try (KafkaConsumer<String, String> dltConsumer = dltConsumer()) {
            dltConsumer.subscribe(List.of(dltTopic));
            dltConsumer.poll(Duration.ofSeconds(2)).forEach(r -> received.add(r.value()));
        }
        return received;
    }

    private RefundRequestedEvent refundEvent(UUID bookingId) {
        return new RefundRequestedEvent(UUID.randomUUID(), Instant.now(), bookingId,
                UUID.randomUUID(), new BigDecimal("500.00"));
    }

    private void savePayment(UUID bookingId, PaymentStatus status) {
        paymentRepository.saveAndFlush(new Payment(UUID.randomUUID(), bookingId, new BigDecimal("500.00"),
                status, UUID.randomUUID(), Instant.now()));
    }

    private String statusOf(UUID bookingId) {
        return jdbc.queryForObject("SELECT status FROM payment WHERE booking_id = ?", String.class, bookingId);
    }

    private KafkaConsumer<String, String> dltConsumer() {
        return new KafkaConsumer<>(Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
    }

    private void sendEvent(String eventType, UUID key, String payload) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key.toString(), payload);
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record);
    }
}
