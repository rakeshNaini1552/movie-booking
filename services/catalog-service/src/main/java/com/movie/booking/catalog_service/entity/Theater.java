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
@Table(name = "theater")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Theater {

    @Id
    private UUID id;

    private String name;

    private String city;

    private String address;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder
    public Theater(UUID id, String name, String city, String address, Instant now) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.address = address;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String city, String address, Instant now) {
        this.name = name;
        this.city = city;
        this.address = address;
        this.updatedAt = now;
    }
}
