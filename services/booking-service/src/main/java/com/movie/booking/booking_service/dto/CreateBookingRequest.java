package com.movie.booking.booking_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(@NotNull UUID userId, @NotNull UUID showId, @NotEmpty List< @Size(max = 3) String> seatIds) {
}
