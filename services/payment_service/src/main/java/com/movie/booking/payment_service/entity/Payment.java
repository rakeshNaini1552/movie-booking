package com.movie.booking.payment_service.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "payment")
public class Payment {
    @Id
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "idempotency_key")
    private UUID idempotencyKey;

    protected Payment(){}

    public Payment(UUID paymentId, UUID bookingId, BigDecimal amount,
                   PaymentStatus status, UUID idempotencyKey, Instant now) {
        this.amount=amount;
        this.paymentId=paymentId;
        this.bookingId=bookingId;
        this.status = status;
        this.idempotencyKey=idempotencyKey;
        this.createdAt= now;
        this.updatedAt=now;
    }

}
