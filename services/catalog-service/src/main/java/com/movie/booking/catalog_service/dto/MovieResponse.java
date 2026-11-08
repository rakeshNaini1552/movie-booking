package com.movie.booking.catalog_service.dto;

import com.movie.booking.catalog_service.MovieStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MovieResponse(UUID id, String title, String description, int durationMinutes, String language,
                            String rating, LocalDate releaseDate, MovieStatus status,
                            Instant createdAt, Instant updatedAt) {}
