package com.movie.booking.payment_service.service;

import com.movie.booking.payment_service.dto.PayCommand;
import com.movie.booking.payment_service.entity.Payment;
import com.movie.booking.payment_service.entity.PaymentStatus;
import com.movie.booking.payment_service.repository.PaymentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final Clock clock;
    private final Counter declinedCounter;

    public PaymentService(PaymentRepository paymentRepository, PaymentGateway paymentGateway,
                          Clock clock, MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
        this.declinedCounter = meterRegistry.counter("payment.failures", "reason", "declined");
    }

    public Payment pay(PayCommand command) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        PaymentStatus status = paymentGateway.pay(command.paymentToken(), command.amount());
        Payment payment = new Payment(UUID.randomUUID(), command.bookingId(), command.amount(), status, command.idempotencyKey(), clock.instant());

        try {
            Payment saved = paymentRepository.save(payment);
            if (saved.getStatus() == PaymentStatus.FAILED) {
                declinedCounter.increment();
            }
            return saved;
        } catch (DataIntegrityViolationException e) {
            return paymentRepository.findByIdempotencyKey(command.idempotencyKey())
                    .orElseThrow(() -> e);
        }
    }
}
