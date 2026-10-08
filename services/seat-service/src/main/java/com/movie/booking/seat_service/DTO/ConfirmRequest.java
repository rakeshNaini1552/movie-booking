package com.movie.booking.seat_service.DTO;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ConfirmRequest(@NotEmpty List<String> seatIds, @NotNull UUID holdId, @NotNull UUID bookingId) {
}
