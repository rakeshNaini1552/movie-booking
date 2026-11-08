package com.movie.booking.catalog_service.entity;

import com.movie.booking.catalog_service.ShowStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "show")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Show {

    @Id
    private UUID id;

    @Column(name = "movie_id")
    private UUID movieId;

    @Column(name = "screen_id")
    private UUID screenId;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    private ShowStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder
    public Show(UUID id, UUID movieId, UUID screenId, Instant startsAt, Instant endsAt,
                BigDecimal basePrice, Instant now) {
        this.id = id;
        this.movieId = movieId;
        this.screenId = screenId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.basePrice = basePrice;
        this.status = ShowStatus.SCHEDULED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(UUID movieId, UUID screenId, Instant startsAt, Instant endsAt,
                       BigDecimal basePrice, Instant now) {
        this.movieId = movieId;
        this.screenId = screenId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.basePrice = basePrice;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        this.status = ShowStatus.CANCELLED;
        this.updatedAt = now;
    }
}
