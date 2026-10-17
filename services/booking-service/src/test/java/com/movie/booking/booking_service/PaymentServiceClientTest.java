package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.PaymentServiceClient;
import com.movie.booking.booking_service.dto.PaymentResult;
import com.movie.booking.booking_service.exception.InvalidPaymentTokenException;
import com.movie.booking.booking_service.exception.PaymentServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PaymentServiceClientTest {

    private static final String BASE_URL = "http://payment-service";
    private static final String PAYMENTS_URL = BASE_URL + "/api/v1/payments";

    private MockRestServiceServer server;
    private PaymentServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PaymentServiceClient(builder.build());
    }

    @Test
    void sends_key_header_and_body_and_returns_the_payment_result() {
        UUID bookingId = UUID.randomUUID();
        UUID key = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        server.expect(requestTo(PAYMENTS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", key.toString()))
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.paymentToken").value("tok_success"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"paymentId\":\"" + paymentId + "\",\"status\":\"SUCCESS\"}"));

        PaymentResult result = client.pay(bookingId, new BigDecimal("500.00"), "tok_success", key);

        assertThat(result.paymentId()).isEqualTo(paymentId);
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        server.verify();
    }

    @Test
    void declined_payment_is_returned_as_failed_not_as_an_exception() {
        UUID paymentId = UUID.randomUUID();

        server.expect(requestTo(PAYMENTS_URL))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"paymentId\":\"" + paymentId + "\",\"status\":\"FAILED\"}"));

        PaymentResult result = client.pay(UUID.randomUUID(), new BigDecimal("500.00"),
                "tok_decline", UUID.randomUUID());

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void bad_request_becomes_InvalidPaymentTokenException() {
        server.expect(requestTo(PAYMENTS_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.pay(UUID.randomUUID(), new BigDecimal("500.00"),
                "tok_banana", UUID.randomUUID()))
                .isInstanceOf(InvalidPaymentTokenException.class);
    }

    @Test
    void server_error_becomes_PaymentServiceUnavailableException() {
        server.expect(requestTo(PAYMENTS_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.pay(UUID.randomUUID(), new BigDecimal("500.00"),
                "tok_success", UUID.randomUUID()))
                .isInstanceOf(PaymentServiceUnavailableException.class)
                .hasCauseInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void connection_failure_becomes_PaymentServiceUnavailableException() {
        server.expect(requestTo(PAYMENTS_URL))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.pay(UUID.randomUUID(), new BigDecimal("500.00"),
                "tok_success", UUID.randomUUID()))
                .isInstanceOf(PaymentServiceUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }
}
