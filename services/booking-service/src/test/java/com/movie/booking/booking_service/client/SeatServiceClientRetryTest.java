package com.movie.booking.booking_service.client;

import com.movie.booking.booking_service.TestcontainersConfiguration;
import com.movie.booking.booking_service.exception.SeatServiceUnavailableException;
import com.movie.booking.booking_service.exception.SeatsUnavailableException;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SeatServiceClientRetryTest {

    private static final AtomicInteger calls = new AtomicInteger();
    private static final AtomicInteger failuresLeft = new AtomicInteger();
    private static final AtomicInteger failureStatus = new AtomicInteger(503);
    private static final HttpServer fakeSeatService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;


    static {
        try {
            fakeSeatService = HttpServer.create(new InetSocketAddress(0), 0);   // port 0 = any free port
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        fakeSeatService.createContext("/", exchange -> {
            calls.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            int status = failuresLeft.getAndUpdate(n -> Math.max(0, n - 1)) > 0
                    ? failureStatus.get()
                    : 201;
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        fakeSeatService.start();
    }

    @DynamicPropertySource
    static void seatServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("seat-service.base-url",
                () -> "http://localhost:" + fakeSeatService.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        fakeSeatService.stop(0);
    }

    @Autowired
    private SeatClient seatClient;

    @BeforeEach
    void reset() {
        calls.set(0);
        failuresLeft.set(0);
        failureStatus.set(503);
        circuitBreakerRegistry.circuitBreaker("seatService").reset();
    }

    @Test
    void circuit_opens_after_repeated_failures_and_stops_calling_seat_service() {
        failuresLeft.set(1000);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() ->
                    seatClient.holdSeats(UUID.randomUUID(), List.of("A1"), UUID.randomUUID()))
                    .isInstanceOf(SeatServiceUnavailableException.class);
        }

        assertThat(circuitBreakerRegistry.circuitBreaker("seatService").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        int callsBeforeRejectedRequest = calls.get();

        assertThatThrownBy(() ->
                seatClient.holdSeats(UUID.randomUUID(), List.of("A1"), UUID.randomUUID()))
                .isInstanceOf(SeatServiceUnavailableException.class);

        assertThat(calls.get()).isEqualTo(callsBeforeRejectedRequest);
    }

    @Test
    void retries_temporary_failures_until_success() {
        failuresLeft.set(2);

        seatClient.holdSeats(UUID.randomUUID(), List.of("A1"), UUID.randomUUID());

        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void gives_up_after_max_attempts() {
        failuresLeft.set(100);

        assertThatThrownBy(() ->
                seatClient.holdSeats(UUID.randomUUID(), List.of("A1"), UUID.randomUUID()))
                .isInstanceOf(SeatServiceUnavailableException.class);

        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void does_not_retry_seats_taken() {
        failureStatus.set(409);
        failuresLeft.set(100);

        assertThatThrownBy(() ->
                seatClient.holdSeats(UUID.randomUUID(), List.of("A1"), UUID.randomUUID()))
                .isInstanceOf(SeatsUnavailableException.class);

        assertThat(calls.get()).isEqualTo(1);
    }
}
