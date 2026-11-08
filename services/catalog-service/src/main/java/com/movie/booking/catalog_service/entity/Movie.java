package com.movie.booking.catalog_service.entity;

import com.movie.booking.catalog_service.MovieStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "movie")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movie {

    @Id
    private UUID id;

    private String title;

    private String description;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    private String language;

    private String rating;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Enumerated(EnumType.STRING)
    private MovieStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder
    public Movie(UUID id, String title, String description, int durationMinutes, String language,
                 String rating, LocalDate releaseDate, MovieStatus status, Instant now) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.language = language;
        this.rating = rating;
        this.releaseDate = releaseDate;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String description, int durationMinutes, String language,
                       String rating, LocalDate releaseDate, MovieStatus status, Instant now) {
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.language = language;
        this.rating = rating;
        this.releaseDate = releaseDate;
        this.status = status;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        this.status = MovieStatus.ARCHIVED;
        this.updatedAt = now;
    }
}
