package com.movie.booking.payment_service.controller;

import com.movie.booking.payment_service.dto.ApiError;
import com.movie.booking.payment_service.dto.PayCommand;
import com.movie.booking.payment_service.dto.PaymentRequest;
import com.movie.booking.payment_service.dto.PaymentResponse;
import com.movie.booking.payment_service.entity.Payment;
import com.movie.booking.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Payments", description = "Mock payments (internal-service role)")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Charge a payment",
            description = "Requires the internal-service role. Repeating a request with the same Idempotency-Key returns the original payment. A declined payment is still 201 with status FAILED.")
    @ApiResponse(responseCode = "400", description = "Invalid payment token, validation failure, or missing/malformed Idempotency-Key", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "403", description = "Token lacks the internal-service role")
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
