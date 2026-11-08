package com.movie.booking.catalog_service.dto;

import java.time.Instant;
import java.util.UUID;

public record TheaterResponse(UUID id, String name, String city, String address,
                              Instant createdAt, Instant updatedAt) {}
