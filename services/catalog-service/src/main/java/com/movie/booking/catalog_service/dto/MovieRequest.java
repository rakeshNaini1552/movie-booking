package com.movie.booking.catalog_service.dto;

import com.movie.booking.catalog_service.FieldLimits;
import com.movie.booking.catalog_service.MovieStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// status is optional: a new movie defaults to ACTIVE and an update without it keeps the current status.
// posterUrl is optional (null clears it) and is either a path on the frontend (/posters/x.jpg) or an https URL.
public record MovieRequest(
        @NotBlank @Size(max = FieldLimits.TITLE) String title,
        String description,
        @NotNull @Positive Integer durationMinutes,
        @NotBlank @Size(max = FieldLimits.LANGUAGE) String language,
        @Size(max = FieldLimits.RATING) String rating,
        LocalDate releaseDate,
        MovieStatus status,
        @Size(max = FieldLimits.POSTER_URL) @Pattern(regexp = "^(/|https://).*", message = "must start with / or https://") String posterUrl) {}
