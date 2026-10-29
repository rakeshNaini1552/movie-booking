package com.movie.booking.payment_service;

import com.movie.booking.payment_service.config.SecurityConfig;
import com.movie.booking.payment_service.controller.PaymentController;
import com.movie.booking.payment_service.entity.Payment;
import com.movie.booking.payment_service.entity.PaymentStatus;
import com.movie.booking.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.movie.booking.payment_service.TestJwt.internalService;
import static com.movie.booking.payment_service.TestJwt.withoutInternalServiceRole;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void payment_without_token_returns_401() throws Exception {
        mockMvc.perform(payRequest())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void payment_with_jwt_lacking_internal_service_role_returns_403() throws Exception {
        mockMvc.perform(payRequest().with(withoutInternalServiceRole()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(paymentService);
    }

    @Test
    void payment_with_internal_service_role_passes_security() throws Exception {
        when(paymentService.pay(any())).thenReturn(new Payment(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("500.00"), PaymentStatus.SUCCESS, UUID.randomUUID(), Instant.now()));

        mockMvc.perform(payRequest().with(internalService()))
                .andExpect(status().isCreated());
    }

    private MockHttpServletRequestBuilder payRequest() {
        return post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .content("{\"bookingId\":\"22222222-2222-4222-8222-222222222222\",\"amount\":500.00,\"paymentToken\":\"tok_success\"}");
    }
}
