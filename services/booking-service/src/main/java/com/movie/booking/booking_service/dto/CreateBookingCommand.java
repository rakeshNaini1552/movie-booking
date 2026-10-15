package com.movie.booking.booking_service.dto;

import java.util.List;
import java.util.UUID;

public record CreateBookingCommand(UUID userId, UUID showId, List<String> seatIds, UUID idempotencyKey) {
}
