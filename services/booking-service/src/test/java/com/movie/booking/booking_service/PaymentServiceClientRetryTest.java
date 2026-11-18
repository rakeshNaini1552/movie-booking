package com.movie.booking.booking_service;

import com.movie.booking.booking_service.client.PaymentClient;
import com.movie.booking.booking_service.dto.PaymentResult;
import com.movie.booking.booking_service.exception.InvalidPaymentTokenException;
import com.movie.booking.booking_service.exception.PaymentServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PaymentServiceClientRetryTest {

    private static final long SHORT_READ_TIMEOUT_MS = 300;
    private static final long SLOW_RESPONSE_MS = 800;

    private static final AtomicInteger calls = new AtomicInteger();
    private static final AtomicInteger failuresLeft = new AtomicInteger();
    private static final AtomicInteger failureStatus = new AtomicInteger(503);
    private static final AtomicInteger slowResponsesLeft = new AtomicInteger();
    private static final List<String> idempotencyKeys = new CopyOnWriteArrayList<>();
    private static final HttpServer fakePaymentService;

    static {
        try {
            fakePaymentService = HttpServer.create(new InetSocketAddress(0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // a pool, so a slow first attempt does not block the retry
        fakePaymentService.setExecutor(Executors.newCachedThreadPool());
        fakePaymentService.createContext("/", exchange -> {
            calls.incrementAndGet();
            idempotencyKeys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            exchange.getRequestBody().readAllBytes();
            if (slowResponsesLeft.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                try {
                    Thread.sleep(SLOW_RESPONSE_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (failuresLeft.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                exchange.sendResponseHeaders(failureStatus.get(), -1);
            } else {
                byte[] body = ("{\"paymentId\":\"" + UUID.randomUUID() + "\",\"status\":\"SUCCESS\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(201, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        fakePaymentService.start();
    }

    @DynamicPropertySource
    static void paymentService(DynamicPropertyRegistry registry) {
        registry.add("payment-service.base-url", () -> "http://localhost:" + fakePaymentService.getAddress().getPort());
        registry.add("payment-service.read-timeout-ms", () -> SHORT_READ_TIMEOUT_MS);
    }

    @AfterAll
    static void stopServer() {
        fakePaymentService.stop(0);
    }

    @Autowired
    private PaymentClient paymentClient;

    @BeforeEach
    void reset() {
        calls.set(0);
        failuresLeft.set(0);
        failureStatus.set(503);
        slowResponsesLeft.set(0);
        idempotencyKeys.clear();
    }

    @Test
    void retries_once_after_a_server_error_with_the_same_idempotency_key() {
        failuresLeft.set(1);
        UUID key = UUID.randomUUID();

        PaymentResult result = pay(key);

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(calls.get()).isEqualTo(2);
        assertThat(idempotencyKeys).containsExactly(key.toString(), key.toString());
    }

    @Test
    void retries_once_after_a_read_timeout_with_the_same_idempotency_key() {
        slowResponsesLeft.set(1);
        UUID key = UUID.randomUUID();

        PaymentResult result = pay(key);

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(calls.get()).isEqualTo(2);
        assertThat(idempotencyKeys).containsExactly(key.toString(), key.toString());
    }

    @Test
    void makes_at_most_one_extra_attempt() {
        failuresLeft.set(100);

        assertThatThrownBy(() -> pay(UUID.randomUUID())).isInstanceOf(PaymentServiceUnavailableException.class);

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void does_not_retry_an_invalid_payment_token() {
        failureStatus.set(400);
        failuresLeft.set(100);

        assertThatThrownBy(() -> pay(UUID.randomUUID())).isInstanceOf(InvalidPaymentTokenException.class);

        assertThat(calls.get()).isEqualTo(1);
    }

    private PaymentResult pay(UUID key) {
        return paymentClient.pay(UUID.randomUUID(), new BigDecimal("500.00"), "tok_success", key);
    }
}
