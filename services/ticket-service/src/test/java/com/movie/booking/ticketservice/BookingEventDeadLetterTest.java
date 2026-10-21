package com.movie.booking.ticketservice;

import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.repository.TicketRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingEventDeadLetterTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private KafkaContainer kafka;

    @Value("${ticket.consumer.topic}")
    private String topic;

    @Value("${ticket.consumer.dlt-topic}")
    private String dltTopic;

    @BeforeEach
    void clean() {
        ticketRepository.deleteAll();
    }

    @Test
    void malformedMessageEndsUpInDeadLetterTopic() {
        String junk = "not-json-" + UUID.randomUUID();

        kafkaTemplate.send(topic, UUID.randomUUID().toString(), junk);

        try (KafkaConsumer<String, String> dltConsumer = dltConsumer()) {
            dltConsumer.subscribe(List.of(dltTopic));
            List<String> received = new ArrayList<>();

            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                dltConsumer.poll(Duration.ofMillis(500)).forEach(r -> received.add(r.value()));
                assertThat(received).contains(junk);
            });
        }
    }

    @Test
    void validMessageAfterMalformedOneStillIssuesTicket() {
        UUID bookingId = UUID.randomUUID();

        kafkaTemplate.send(topic, UUID.randomUUID().toString(), "not-json-" + UUID.randomUUID());
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                UUID.randomUUID(), Instant.now(), bookingId,
                UUID.randomUUID(), UUID.randomUUID(), List.of("A1"));
        kafkaTemplate.send(topic, bookingId.toString(), jsonMapper.writeValueAsString(event));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(ticketRepository.findByBookingId(bookingId)).isPresent());
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
