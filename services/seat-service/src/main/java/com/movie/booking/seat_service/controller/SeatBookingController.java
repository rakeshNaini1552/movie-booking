package com.movie.booking.seat_service.controller;

import com.movie.booking.seat_service.dto.BookingResponse;
import com.movie.booking.seat_service.dto.ConfirmRequest;
import com.movie.booking.seat_service.service.SeatConfirmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
public class SeatBookingController {

    private final SeatConfirmService seatConfirmService;

    @PostMapping("/{showId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse confirm(@PathVariable UUID showId,
                                   @RequestBody @Valid ConfirmRequest request) {
        seatConfirmService.confirm(showId, request.seatIds(), request.bookingId());
        return new BookingResponse(request.bookingId());
    }
}
