package com.movie.booking.ticketservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ScanRequest(@NotBlank String payload) {
}
