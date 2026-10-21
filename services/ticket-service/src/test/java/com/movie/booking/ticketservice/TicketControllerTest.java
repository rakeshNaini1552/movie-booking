package com.movie.booking.ticketservice;

import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.entity.TicketStatus;
import com.movie.booking.ticketservice.repository.OutboxRepository;
import com.movie.booking.ticketservice.repository.TicketRepository;
import com.movie.booking.ticketservice.service.QrSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private QrSigner qrSigner;

    @BeforeEach
    void clean() {
        ticketRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    @Test
    void getByBookingReturnsTicket() throws Exception {
        Ticket ticket = saveTicket();

        mockMvc.perform(get("/tickets/by-booking/{id}", ticket.getBookingId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticket.getTicketId().toString()))
                .andExpect(jsonPath("$.bookingId").value(ticket.getBookingId().toString()))
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.qrPayload").value(ticket.getQrPayload()))
                .andExpect(jsonPath("$.issuedAt").exists())
                .andExpect(jsonPath("$.usedAt").doesNotExist());
    }

    @Test
    void getByBookingIs404WhenNotIssuedYet() throws Exception {
        mockMvc.perform(get("/tickets/by-booking/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByTicketIdReturnsTicketOr404() throws Exception {
        Ticket ticket = saveTicket();

        mockMvc.perform(get("/tickets/{id}", ticket.getTicketId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticket.getTicketId().toString()));
        mockMvc.perform(get("/tickets/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void firstValidScanMarksTicketUsed() throws Exception {
        Ticket ticket = saveTicket();

        scan(ticket.getTicketId(), ticket.getQrPayload())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticket.getTicketId().toString()))
                .andExpect(jsonPath("$.status").value("USED"))
                .andExpect(jsonPath("$.usedAt").exists());

        Ticket reloaded = ticketRepository.findById(ticket.getTicketId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(reloaded.getUsedAt()).isNotNull();
    }

    @Test
    void secondScanIs409AndReportsUsedAt() throws Exception {
        Ticket ticket = saveTicket();
        scan(ticket.getTicketId(), ticket.getQrPayload()).andExpect(status().isOk());
        Instant usedAt = ticketRepository.findById(ticket.getTicketId()).orElseThrow().getUsedAt();

        scan(ticket.getTicketId(), ticket.getQrPayload())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already used")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(usedAt.toString())));
    }

    @Test
    void voidTicketIs409WithDistinctMessage() throws Exception {
        Ticket ticket = saveTicketWithStatus(TicketStatus.VOID);

        scan(ticket.getTicketId(), ticket.getQrPayload())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("VOID")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("already used"))));
        assertThat(ticketRepository.findById(ticket.getTicketId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.VOID);
    }

    @Test
    void tamperedSignatureIs400AndTicketStaysIssued() throws Exception {
        Ticket ticket = saveTicket();

        scan(ticket.getTicketId(), ticket.getTicketId() + ".forged-signature")
                .andExpect(status().isBadRequest());
        assertThat(ticketRepository.findById(ticket.getTicketId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.ISSUED);
    }

    @Test
    void malformedPayloadIs400() throws Exception {
        Ticket ticket = saveTicket();

        scan(ticket.getTicketId(), "not-a-qr").andExpect(status().isBadRequest());
        mockMvc.perform(post("/tickets/{id}/scan", ticket.getTicketId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validPayloadOfAnotherTicketIs400() throws Exception {
        Ticket first = saveTicket();
        Ticket second = saveTicket();

        scan(first.getTicketId(), second.getQrPayload()).andExpect(status().isBadRequest());

        assertThat(ticketRepository.findById(first.getTicketId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.ISSUED);
        assertThat(ticketRepository.findById(second.getTicketId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.ISSUED);
    }

    @Test
    void scanOfUnknownTicketIs404() throws Exception {
        UUID unknown = UUID.randomUUID();

        scan(unknown, qrSigner.sign(unknown)).andExpect(status().isNotFound());
    }

    @Test
    void twoSimultaneousScansAdmitExactlyOnce() throws Exception {
        Ticket ticket = saveTicket();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> scanCall = () -> {
            start.await();
            return scan(ticket.getTicketId(), ticket.getQrPayload())
                    .andReturn().getResponse().getStatus();
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> a = pool.submit(scanCall);
            Future<Integer> b = pool.submit(scanCall);
            start.countDown();

            assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(ticketRepository.findById(ticket.getTicketId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.USED);
    }

    private org.springframework.test.web.servlet.ResultActions scan(UUID ticketId, String payload) throws Exception {
        return mockMvc.perform(post("/tickets/{id}/scan", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"payload\":\"" + payload + "\"}"));
    }

    private Ticket saveTicket() {
        UUID ticketId = UUID.randomUUID();
        return ticketRepository.saveAndFlush(new Ticket(ticketId, UUID.randomUUID(), UUID.randomUUID(),
                qrSigner.sign(ticketId), Instant.now()));
    }

    private Ticket saveTicketWithStatus(TicketStatus status) {
        Ticket ticket = saveTicket();
        // entities have no setters, so move the row to the wanted state with SQL-level update
        if (status == TicketStatus.VOID) {
            voidTicket(ticket.getTicketId());
        }
        return ticketRepository.findById(ticket.getTicketId()).orElseThrow();
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private void voidTicket(UUID ticketId) {
        jdbcTemplate.update("update ticket set status = 'VOID' where ticket_id = ?", ticketId);
    }
}
