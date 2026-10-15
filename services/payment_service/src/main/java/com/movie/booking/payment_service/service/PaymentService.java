package com.movie.booking.payment_service.service;

import com.movie.booking.payment_service.dto.PayCommand;
import com.movie.booking.payment_service.entity.Payment;
import com.movie.booking.payment_service.entity.PaymentStatus;
import com.movie.booking.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final Clock clock;

    public Payment pay(PayCommand command) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        PaymentStatus status = paymentGateway.pay(command.paymentToken(), command.amount());
        Payment payment = new Payment(UUID.randomUUID(), command.bookingId(), command.amount(), status, command.idempotencyKey(), clock.instant());

        try {
            return paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            return paymentRepository.findByIdempotencyKey(command.idempotencyKey())
                    .orElseThrow(() -> e);
        }
    }
}
