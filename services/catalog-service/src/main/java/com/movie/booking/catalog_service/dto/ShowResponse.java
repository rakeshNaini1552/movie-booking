package com.movie.booking.catalog_service.dto;

import com.movie.booking.catalog_service.ShowStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShowResponse(UUID id, UUID movieId, UUID screenId, Instant startsAt, Instant endsAt,
                           BigDecimal basePrice, ShowStatus status, Instant createdAt, Instant updatedAt) {}
