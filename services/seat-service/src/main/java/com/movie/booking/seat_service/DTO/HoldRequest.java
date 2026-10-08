package com.movie.booking.seat_service.DTO;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record HoldRequest(@NotEmpty List<String> seatIds) {}

