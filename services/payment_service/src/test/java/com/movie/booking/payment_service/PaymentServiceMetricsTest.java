package com.movie.booking.payment_service;

import com.movie.booking.payment_service.dto.PayCommand;
import com.movie.booking.payment_service.entity.Payment;
import com.movie.booking.payment_service.entity.PaymentStatus;
import com.movie.booking.payment_service.exception.InvalidPaymentTokenException;
import com.movie.booking.payment_service.repository.PaymentRepository;
import com.movie.booking.payment_service.service.PaymentGateway;
import com.movie.booking.payment_service.service.PaymentService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentServiceMetricsTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentGateway paymentGateway = mock(PaymentGateway.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final PaymentService service =
            new PaymentService(paymentRepository, paymentGateway, Clock.systemUTC(), registry);

    private final UUID key = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("500.00");
    private final PayCommand command = new PayCommand(UUID.randomUUID(), amount, "tok", key);

    private double declined() {
        return registry.counter("payment.failures", "reason", "declined").count();
    }

    private Payment payment(PaymentStatus status) {
        return new Payment(UUID.randomUUID(), command.bookingId(), amount, status, key, Instant.now());
    }

    @Test
    void declined_payment_increments_the_declined_counter_after_save() {
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(paymentGateway.pay("tok", amount)).thenReturn(PaymentStatus.FAILED);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.pay(command);

        assertThat(declined()).isEqualTo(1.0);
    }

    @Test
    void successful_payment_does_not_increment() {
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(paymentGateway.pay("tok", amount)).thenReturn(PaymentStatus.SUCCESS);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.pay(command);

        assertThat(declined()).isEqualTo(0.0);
    }

    @Test
    void replay_of_an_already_failed_payment_does_not_increment() {
        when(paymentRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(payment(PaymentStatus.FAILED)));

        service.pay(command);

        assertThat(declined()).isEqualTo(0.0);
    }

    @Test
    void losing_the_idempotency_race_does_not_increment() {
        when(paymentRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(payment(PaymentStatus.FAILED)));
        when(paymentGateway.pay("tok", amount)).thenReturn(PaymentStatus.FAILED);
        when(paymentRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

        service.pay(command);

        assertThat(declined()).isEqualTo(0.0);
    }

    @Test
    void invalid_token_persists_nothing_and_does_not_increment() {
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(paymentGateway.pay("tok", amount)).thenThrow(new InvalidPaymentTokenException("bad"));

        assertThatThrownBy(() -> service.pay(command)).isInstanceOf(InvalidPaymentTokenException.class);

        assertThat(declined()).isEqualTo(0.0);
    }
}
