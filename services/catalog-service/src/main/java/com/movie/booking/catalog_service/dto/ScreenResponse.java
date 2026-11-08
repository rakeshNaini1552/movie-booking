package com.movie.booking.catalog_service.dto;

import java.time.Instant;
import java.util.UUID;

public record ScreenResponse(UUID id, UUID theaterId, String name, int seatRows, int seatsPerRow,
                             Instant createdAt, Instant updatedAt) {}
