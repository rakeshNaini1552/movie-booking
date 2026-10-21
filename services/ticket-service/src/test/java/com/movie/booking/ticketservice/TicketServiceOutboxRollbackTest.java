package com.movie.booking.ticketservice;

import com.movie.booking.ticketservice.event.BookingConfirmedEvent;
import com.movie.booking.ticketservice.repository.OutboxRepository;
import com.movie.booking.ticketservice.repository.TicketRepository;
import com.movie.booking.ticketservice.service.OutboxService;
import com.movie.booking.ticketservice.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TicketServiceOutboxRollbackTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockitoSpyBean
    private OutboxService outboxService;

    @BeforeEach
    void clean() {
        ticketRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    @Test
    void ticketIsRolledBackWhenOutboxWriteFails() {
        doThrow(new IllegalStateException("outbox down"))
                .when(outboxService).enqueue(any(), any(), any(), any());

        BookingConfirmedEvent event = new BookingConfirmedEvent(
                UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), List.of("A1"));

        assertThatThrownBy(() -> ticketService.issueTicket(event))
                .isInstanceOf(IllegalStateException.class);

        assertThat(ticketRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
    }
}
