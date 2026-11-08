package com.movie.booking.catalog_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShowRequest(
        @NotNull UUID movieId,
        @NotNull UUID screenId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal basePrice) {

    // Skipped (true) when either time is missing: @NotNull already reports that.
    @AssertTrue(message = "endsAt must be after startsAt")
    @JsonIgnore
    public boolean isEndsAfterStart() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
