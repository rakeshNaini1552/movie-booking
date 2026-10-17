package com.movie.booking.booking_service.client;

import java.util.List;
import java.util.UUID;

public interface SeatClient {
    void holdSeats(UUID showId, List<String> seatIds, UUID bookingId);
    // SeatClient interface
    void confirmSeats(UUID showId, List<String> seatIds, UUID bookingId);
}
