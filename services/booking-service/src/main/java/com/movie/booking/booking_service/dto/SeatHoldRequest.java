package com.movie.booking.booking_service.dto;

import java.util.List;
import java.util.UUID;

public record SeatHoldRequest(List<String> seatIds, UUID bookingId) {}
