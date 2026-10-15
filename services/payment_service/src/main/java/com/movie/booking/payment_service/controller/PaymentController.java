package com.movie.booking.payment_service.controller;

import com.movie.booking.payment_service.dto.PayCommand;
import com.movie.booking.payment_service.dto.PaymentRequest;
import com.movie.booking.payment_service.dto.PaymentResponse;
import com.movie.booking.payment_service.entity.Payment;
import com.movie.booking.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentService.pay(new PayCommand(
                request.bookingId(),
                request.amount(),
                request.paymentToken(),
                idempotencyKey));
        return new PaymentResponse(payment.getPaymentId(), payment.getStatus());
    }
}
