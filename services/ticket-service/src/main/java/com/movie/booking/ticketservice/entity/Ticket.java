package com.movie.booking.ticketservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "ticket")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    @Id
    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "qr_payload")
    private String qrPayload;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Builder
    public Ticket(UUID ticketId, UUID bookingId, UUID eventId, String qrPayload, Instant issuedAt) {
        this.ticketId = ticketId;
        this.bookingId = bookingId;
        this.eventId = eventId;
        this.qrPayload = qrPayload;
        this.issuedAt = issuedAt;
        this.status = TicketStatus.ISSUED;
    }

}

