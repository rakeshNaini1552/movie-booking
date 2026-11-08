package com.movie.booking.catalog_service.dto;

import com.movie.booking.catalog_service.FieldLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ScreenRequest(
        @NotBlank @Size(max = FieldLimits.SCREEN_NAME) String name,
        @NotNull @Positive @Max(FieldLimits.MAX_SEAT_ROWS) Integer seatRows,
        @NotNull @Positive @Max(FieldLimits.MAX_SEATS_PER_ROW) Integer seatsPerRow) {}
