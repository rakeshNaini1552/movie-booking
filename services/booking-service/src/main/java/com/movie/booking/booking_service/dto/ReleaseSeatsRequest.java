package com.movie.booking.booking_service.dto;

import java.util.List;

public record ReleaseSeatsRequest(List<String> seatIds) {}
