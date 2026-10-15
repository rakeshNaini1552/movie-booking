package com.movie.booking.payment_service.controller;

import com.movie.booking.payment_service.entity.Payment;
import com.movie.booking.payment_service.entity.PaymentStatus;
import com.movie.booking.payment_service.exception.InvalidPaymentTokenException;
import com.movie.booking.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private static final String BODY = """
            {"bookingId":"22222222-2222-4222-8222-222222222222","amount":500.00,"paymentToken":"tok_success"}
            """;

    @Test
    void valid_request_returns_201_with_payment_status() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(paymentId, UUID.randomUUID(), new BigDecimal("500.00"),
                PaymentStatus.SUCCESS, UUID.randomUUID(), Instant.now());
        when(paymentService.pay(any())).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void invalid_token_returns_400_with_fixed_message() throws Exception {
        when(paymentService.pay(any())).thenThrow(new InvalidPaymentTokenException("Unknown payment token"));

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid payment token"));
    }

    @Test
    void missing_idempotency_key_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void non_positive_amount_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content("""
                                {"bookingId":"22222222-2222-4222-8222-222222222222","amount":0,"paymentToken":"tok_success"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }
}
