package com.movie.booking.seat_service.consumer;

import com.movie.booking.seat_service.SeatStatus;
import com.movie.booking.seat_service.SeatType;
import com.movie.booking.seat_service.TestContainersConfig;
import com.movie.booking.seat_service.event.BookingCancelledEvent;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestContainersConfig.class)
class BookingCancelledConsumerTest {

    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper jsonMapper;
    @Autowired KafkaContainer kafka;

    @Value("${seat.consumer.topic}") String topic;
    @Value("${seat.consumer.dlt-topic}") String dltTopic;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM show_seat");
    }

    @Test
    void bookingCancelledFreesTheBookedSeats() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.BOOKED, bookingId);
        insertSeat(showId, "A2", SeatStatus.BOOKED, bookingId);
        long versionBefore = versionOf(showId, "A1");

        send("BookingCancelled", cancelled(bookingId, showId, "A1", "A2"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(statusOf(showId, "A1")).isEqualTo("AVAILABLE");
            assertThat(statusOf(showId, "A2")).isEqualTo("AVAILABLE");
        });
        assertThat(bookingIdOf(showId, "A1")).isNull();
        assertThat(versionOf(showId, "A1")).isEqualTo(versionBefore + 1);
    }

    @Test
    void duplicateDeliveryChangesNothingTheSecondTime() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.BOOKED, bookingId);
        String event = cancelled(bookingId, showId, "A1");

        send("BookingCancelled", event);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(statusOf(showId, "A1")).isEqualTo("AVAILABLE"));
        long versionAfterFirst = versionOf(showId, "A1");

        // Same event again, followed by a marker event so we know the duplicate was consumed.
        send("BookingCancelled", event);
        UUID markerBooking = UUID.randomUUID();
        insertSeat(showId, "B1", SeatStatus.BOOKED, markerBooking);
        send("BookingCancelled", cancelled(markerBooking, showId, "B1"));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(statusOf(showId, "B1")).isEqualTo("AVAILABLE"));

        assertThat(versionOf(showId, "A1")).isEqualTo(versionAfterFirst);
    }

    @Test
    void seatsNowBookedByAnotherBookingAreLeftAlone() {
        UUID showId = UUID.randomUUID();
        UUID cancelledBooking = UUID.randomUUID();
        UUID otherBooking = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.BOOKED, otherBooking);
        UUID markerBooking = UUID.randomUUID();
        insertSeat(showId, "B1", SeatStatus.BOOKED, markerBooking);

        send("BookingCancelled", cancelled(cancelledBooking, showId, "A1"));
        send("BookingCancelled", cancelled(markerBooking, showId, "B1"));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(statusOf(showId, "B1")).isEqualTo("AVAILABLE"));

        assertThat(statusOf(showId, "A1")).isEqualTo("BOOKED");
        assertThat(bookingIdOf(showId, "A1")).isEqualTo(otherBooking);
    }

    @Test
    void otherEventTypesDoNotReleaseSeats() {
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.BOOKED, bookingId);
        UUID markerBooking = UUID.randomUUID();
        insertSeat(showId, "B1", SeatStatus.BOOKED, markerBooking);

        send("BookingConfirmed", cancelled(bookingId, showId, "A1"));
        send("RefundRequested", cancelled(bookingId, showId, "A1"));
        send("BookingCancelled", cancelled(markerBooking, showId, "B1"));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(statusOf(showId, "B1")).isEqualTo("AVAILABLE"));

        assertThat(statusOf(showId, "A1")).isEqualTo("BOOKED");
    }

    @Test
    void malformedCancelledMessageGoesToDeadLetterTopicAndConsumerKeepsWorking() {
        String junk = "not-json-" + UUID.randomUUID();
        UUID showId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        insertSeat(showId, "A1", SeatStatus.BOOKED, bookingId);

        send("BookingCancelled", junk);
        send("BookingCancelled", cancelled(bookingId, showId, "A1"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(statusOf(showId, "A1")).isEqualTo("AVAILABLE"));
        try (KafkaConsumer<String, String> dltConsumer = dltConsumer()) {
            dltConsumer.subscribe(List.of(dltTopic));
            List<String> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                dltConsumer.poll(Duration.ofMillis(500)).forEach(r -> received.add(r.value()));
                assertThat(received).contains(junk);
            });
        }
    }

    private void send(String eventType, String payload) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, UUID.randomUUID().toString(), payload);
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record);
    }

    private String cancelled(UUID bookingId, UUID showId, String... seatIds) {
        return jsonMapper.writeValueAsString(new BookingCancelledEvent(
                UUID.randomUUID(), Instant.now(), bookingId, UUID.randomUUID(), showId, List.of(seatIds)));
    }

    private void insertSeat(UUID showId, String seatId, SeatStatus status, UUID bookingId) {
        jdbc.update("INSERT INTO show_seat (show_id, seat_id, seat_row, seat_number, seat_type, status, booking_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                showId, seatId, seatId.substring(0, 1), Integer.parseInt(seatId.substring(1)),
                SeatType.GOLD.name(), status.name(), bookingId);
    }

    private String statusOf(UUID showId, String seatId) {
        return jdbc.queryForObject("SELECT status FROM show_seat WHERE show_id = ? AND seat_id = ?",
                String.class, showId, seatId);
    }

    private UUID bookingIdOf(UUID showId, String seatId) {
        return jdbc.queryForObject("SELECT booking_id FROM show_seat WHERE show_id = ? AND seat_id = ?",
                UUID.class, showId, seatId);
    }

    private long versionOf(UUID showId, String seatId) {
        return jdbc.queryForObject("SELECT version FROM show_seat WHERE show_id = ? AND seat_id = ?",
                Long.class, showId, seatId);
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
