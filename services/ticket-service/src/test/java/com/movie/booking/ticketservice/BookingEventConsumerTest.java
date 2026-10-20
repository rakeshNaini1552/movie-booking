package com.movie.booking.ticketservice;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.entity.TicketStatus;
import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.repository.TicketRepository;
import com.movie.booking.ticketservice.service.QrSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingEventConsumerTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private QrSigner qrSigner;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${ticket.consumer.topic}")
    private String topic;

    @BeforeEach
    void clean() {
        ticketRepository.deleteAll();
    }

    @Test
    void bookingConfirmedMessageIssuesSignedTicket() {
        UUID bookingId = UUID.randomUUID();

        send(bookingId);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(ticketRepository.findByBookingId(bookingId)).isPresent());

        Ticket ticket = ticketRepository.findByBookingId(bookingId).orElseThrow();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ISSUED);
        assertThat(qrSigner.verify(ticket.getQrPayload())).contains(ticket.getTicketId());
    }

    @Test
    void duplicateMessageDoesNotCreateSecondTicket() {
        UUID bookingId = UUID.randomUUID();
        UUID markerBookingId = UUID.randomUUID();

        send(bookingId);
        send(bookingId);
        send(markerBookingId);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(ticketRepository.findByBookingId(markerBookingId)).isPresent());

        assertThat(ticketRepository.count()).isEqualTo(2);
    }

    private void send(UUID bookingId) {
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                UUID.randomUUID(), Instant.now(), bookingId,
                UUID.randomUUID(), UUID.randomUUID(), List.of("A1"));
        kafkaTemplate.send(topic, bookingId.toString(), jsonMapper.writeValueAsString(event));
    }
}
