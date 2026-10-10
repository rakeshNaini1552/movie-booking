package com.movie.booking.seat_service.DTO;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record HoldRequest(@NotEmpty List<String> seatIds) {}

