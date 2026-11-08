package com.movie.booking.catalog_service.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "screen")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Screen {

    @Id
    private UUID id;

    @Column(name = "theater_id")
    private UUID theaterId;

    private String name;

    @Column(name = "seat_rows")
    private int seatRows;

    @Column(name = "seats_per_row")
    private int seatsPerRow;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder
    public Screen(UUID id, UUID theaterId, String name, int seatRows, int seatsPerRow, Instant now) {
        this.id = id;
        this.theaterId = theaterId;
        this.name = name;
        this.seatRows = seatRows;
        this.seatsPerRow = seatsPerRow;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, int seatRows, int seatsPerRow, Instant now) {
        this.name = name;
        this.seatRows = seatRows;
        this.seatsPerRow = seatsPerRow;
        this.updatedAt = now;
    }
}
