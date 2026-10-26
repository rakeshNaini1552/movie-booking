package com.movie.booking.ticketservice;

import com.movie.booking.ticketservice.config.SecurityConfig;
import com.movie.booking.ticketservice.controller.TicketController;
import com.movie.booking.ticketservice.entity.Ticket;
import com.movie.booking.ticketservice.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import(SecurityConfig.class)
class TicketControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final UUID ticketId = UUID.randomUUID();
    private final Ticket ticket = new Ticket(ticketId, UUID.randomUUID(), UUID.randomUUID(),
            ticketId + ".signature", Instant.now());

    @Test
    void get_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/tickets/{id}", ticketId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ticketService);
    }

    @Test
    void get_with_token_that_has_no_roles_returns_200() throws Exception {
        when(ticketService.getTicket(ticketId)).thenReturn(ticket);

        mockMvc.perform(get("/tickets/{id}", ticketId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticketId.toString()));
    }

    @Test
    void scan_without_token_returns_401() throws Exception {
        mockMvc.perform(scanRequest())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ticketService);
    }

    @Test
    void scan_with_customer_only_token_returns_403() throws Exception {
        mockMvc.perform(scanRequest().with(role("customer")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ticketService);
    }

    @Test
    void scan_with_staff_token_succeeds() throws Exception {
        when(ticketService.scan(any(), any())).thenReturn(ticket);

        mockMvc.perform(scanRequest().with(role("staff")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticketId.toString()));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder scanRequest() {
        return post("/tickets/{id}/scan", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"payload\":\"" + ticket.getQrPayload() + "\"}");
    }

    private static RequestPostProcessor role(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
