package com.movie.booking.seat_service.controller;

import com.movie.booking.seat_service.dto.ExtendRequest;
import com.movie.booking.seat_service.dto.HoldRequest;
import com.movie.booking.seat_service.service.SeatHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shows")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    @PostMapping("/{showId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public void hold(@PathVariable UUID showId,
                     @RequestBody @Valid HoldRequest request) {
        seatHoldService.holdSeats(showId, request.seatIds(), request.bookingId());
    }

    @PostMapping("/{showId}/holds/{bookingId}/extend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void extendHold(@PathVariable UUID showId,
                           @PathVariable UUID bookingId,
                           @RequestBody @Valid ExtendRequest request) {
        seatHoldService.extendHold(showId, request.seatIds(), bookingId);
    }
}
