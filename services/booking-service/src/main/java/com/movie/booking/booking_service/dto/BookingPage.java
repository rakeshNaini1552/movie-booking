package com.movie.booking.booking_service.dto;

import java.util.List;

public record BookingPage(List<BookingSummary> items, int page, int size,
                          long totalElements, int totalPages) {}
