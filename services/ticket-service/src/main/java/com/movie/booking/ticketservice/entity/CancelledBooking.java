package com.movie.booking.ticketservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "cancelled_booking")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CancelledBooking {

    @Id
    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public CancelledBooking(UUID bookingId, Instant cancelledAt) {
        this.bookingId = bookingId;
        this.cancelledAt = cancelledAt;
    }
}
