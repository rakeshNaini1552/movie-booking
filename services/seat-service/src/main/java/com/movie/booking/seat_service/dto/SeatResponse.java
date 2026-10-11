package com.movie.booking.seat_service.dto;

import com.movie.booking.seat_service.SeatStatus;
import com.movie.booking.seat_service.SeatType;
import com.movie.booking.seat_service.entity.ShowSeatId;
import com.movie.booking.seat_service.entity.ShowSeat;

public record SeatResponse(String seatId, String row, int number, SeatType type, SeatStatus status) {

    public static SeatResponse from(ShowSeat s, boolean held) {
        ShowSeatId showSeatId = s.getId();
        return new SeatResponse(showSeatId.getSeatId(), s.getRow(), s.getNumber(), s.getType(), s.getStatus());
    }
}
