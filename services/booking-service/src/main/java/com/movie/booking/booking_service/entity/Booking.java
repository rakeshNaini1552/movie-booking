package com.movie.booking.booking_service.entity;

import com.movie.booking.booking_service.BookingStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "show_id")
    private UUID showId;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "idempotency_key")
    private UUID idempotencyKey;

    protected Booking() {}

    public Booking(UUID bookingId, UUID userId, UUID showId, BigDecimal totalAmount, Instant now, UUID idempotencyKey) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.showId = showId;
        this.totalAmount = totalAmount;
        this.status = BookingStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
        this.idempotencyKey = idempotencyKey;
    }

    public void markFailed(Instant now) {
        if (this.status != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Booking " + bookingId + " cannot be marked FAILED from status " + status);
        }
        this.status = BookingStatus.FAILED;
        this.updatedAt = now;
    }

}
