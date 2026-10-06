package com.movie.booking.seat_service;

import com.movie.booking.seat_service.entity.ShowSeat;

public record SeatResponse(String seatId, String row, int number, SeatType type, SeatStatus status) {

    public static SeatResponse from(ShowSeat s) {
        ShowSeatId showSeatId = s.getId();
        return new SeatResponse(showSeatId.getSeatId(), s.getRow(), s.getNumber(), s.getType(), s.getStatus());
    }
}
