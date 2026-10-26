package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.SeatClient;
import com.movie.booking.booking_service.entity.OutboxEvent;
import com.movie.booking.booking_service.event.RefundRequestedEvent;
import com.movie.booking.booking_service.exception.SeatConfirmationFailedException;
import com.movie.booking.booking_service.repository.OutboxRepository;
import com.sun.net.httpserver.HttpServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.movie.booking.booking_service.TestAuth.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Booking-service half of the "paid, then seats fail" saga: the payment succeeds over real HTTP, seat
 * confirm fails, and a RefundRequested event really reaches Kafka. The payment-service half is
 * RefundRequestedConsumerTest, which consumes the same golden JSON. Keep GOLDEN_REFUND_REQUESTED
 * identical in both tests.
 */
@SpringBootTest(properties = {
        "booking.outbox.publisher.enabled=true",
        "booking.outbox.poll-interval-ms=200",
        "booking.expiry.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SagaRefundRequestedPublishedTest {

    static final String GOLDEN_REFUND_REQUESTED = """
            {"eventId":"11111111-1111-4111-8111-111111111111","occurredAt":"2026-01-01T00:00:00Z",\
            "bookingId":"22222222-2222-4222-8222-222222222222","userId":"33333333-3333-4333-8333-333333333333",\
            "amount":500.00}""";

    private static final HttpServer fakePaymentService = startFakePaymentService();

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired OutboxRepository outboxRepository;
    @Autowired JsonMapper jsonMapper;
    @Autowired KafkaContainer kafka;
    @MockitoBean SeatClient seatClient;

    @Value("${booking.outbox.topic}") String topic;

    private final UUID owner = UUID.randomUUID();

    @DynamicPropertySource
    static void paymentServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("payment-service.base-url",
                () -> "http://localhost:" + fakePaymentService.getAddress().getPort());
    }

    @AfterAll
    static void stopFakePaymentService() {
        fakePaymentService.stop(0);
    }

    @BeforeEach
    void cleanUp() {
        reset(seatClient);
        outboxRepository.deleteAll();
        jdbc.update("DELETE FROM booking_seat");
        jdbc.update("DELETE FROM booking");
    }

    @Test
    void paidThenSeatConfirmFailsEndsFailedAndRefundRequestedReachesKafka() throws Exception {
        UUID bookingId = insertPendingBooking("A1", "A2");
        doThrow(new SeatConfirmationFailedException("hold expired"))
                .when(seatClient).confirmSeats(any(), any(), any());

        mockMvc.perform(post("/api/v1/bookings/{id}/payments", bookingId).with(asUser(owner))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentToken\":\"tok_success\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        assertThat(statusOf(bookingId)).isEqualTo("FAILED");
        assertThat(outboxRepository.findAll()).extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("BookingCancelled", "RefundRequested");

        ConsumerRecord<String, String> refund = awaitRefundRequestedOnKafka(bookingId);
        assertThat(refund.key()).isEqualTo(bookingId.toString());
        JsonNode json = jsonMapper.readTree(refund.value());
        assertThat(json.get("bookingId").asString()).isEqualTo(bookingId.toString());
        assertThat(json.get("userId").asString()).isEqualTo(owner.toString());
        assertThat(json.get("amount").decimalValue()).isEqualByComparingTo("500.00");
        assertSameShapeAsGolden(json);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(outboxRepository.findAll()).allSatisfy(row -> assertThat(row.getSentAt()).isNotNull()));
    }

    @Test
    void refundRequestedEventSerialisesToTheGoldenContract() throws Exception {
        RefundRequestedEvent event = new RefundRequestedEvent(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                Instant.parse("2026-01-01T00:00:00Z"),
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                UUID.fromString("33333333-3333-4333-8333-333333333333"),
                new BigDecimal("500.00"));

        JsonNode actual = jsonMapper.readTree(jsonMapper.writeValueAsString(event));
        JsonNode golden = jsonMapper.readTree(GOLDEN_REFUND_REQUESTED);

        assertSameShapeAsGolden(actual);
        for (String field : List.of("eventId", "occurredAt", "bookingId", "userId")) {
            assertThat(actual.get(field).asString()).as(field).isEqualTo(golden.get(field).asString());
        }
        assertThat(actual.get("amount").decimalValue()).isEqualByComparingTo(golden.get("amount").decimalValue());
    }

    private void assertSameShapeAsGolden(JsonNode json) throws Exception {
        JsonNode golden = jsonMapper.readTree(GOLDEN_REFUND_REQUESTED);
        assertThat(Set.copyOf(json.propertyNames())).isEqualTo(Set.copyOf(golden.propertyNames()));
        assertThat(json.get("occurredAt").isString()).isEqualTo(golden.get("occurredAt").isString());
        assertThat(json.get("amount").isNumber()).isTrue();
    }

    private ConsumerRecord<String, String> awaitRefundRequestedOnKafka(UUID bookingId) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "saga-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of(topic));
            List<ConsumerRecord<String, String>> matches = new ArrayList<>();
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(r -> {
                    var header = r.headers().lastHeader("eventType");
                    if (header != null && "RefundRequested".equals(new String(header.value(), StandardCharsets.UTF_8))
                            && bookingId.toString().equals(r.key())) {
                        matches.add(r);
                    }
                });
                assertThat(matches).isNotEmpty();
            });
            return matches.get(0);
        }
    }

    private String statusOf(UUID bookingId) {
        return jdbc.queryForObject("SELECT status FROM booking WHERE booking_id = ?", String.class, bookingId);
    }

    private UUID insertPendingBooking(String... seatIds) {
        UUID bookingId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO booking (booking_id, user_id, show_id, status, total_amount,
                                     created_at, updated_at, idempotency_key)
                VALUES (?, ?, ?, 'PENDING', 500.00, ?, ?, ?)
                """,
                bookingId, owner, UUID.randomUUID(), now, now, UUID.randomUUID());
        for (String seatId : seatIds) {
            jdbc.update("INSERT INTO booking_seat (booking_id, seat_id) VALUES (?, ?)", bookingId, seatId);
        }
        return bookingId;
    }

    private static HttpServer startFakePaymentService() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/v1/payments", exchange -> {
                exchange.getRequestBody().readAllBytes();
                byte[] body = ("{\"paymentId\":\"" + UUID.randomUUID() + "\",\"status\":\"SUCCESS\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(201, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
