package com.movie.booking.seat_service.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReleaseRequest(@NotEmpty List<String> seatIds) {}
