package com.movie.booking.catalog_service.dto;

import com.movie.booking.catalog_service.FieldLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TheaterRequest(
        @NotBlank @Size(max = FieldLimits.THEATER_NAME) String name,
        @NotBlank @Size(max = FieldLimits.CITY) String city,
        @NotBlank @Size(max = FieldLimits.ADDRESS) String address) {}
