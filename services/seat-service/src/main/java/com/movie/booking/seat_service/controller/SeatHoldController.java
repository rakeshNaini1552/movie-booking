package com.movie.booking.seat_service.controller;

import com.movie.booking.seat_service.DTO.HoldRequest;
import com.movie.booking.seat_service.DTO.HoldResponse;
import com.movie.booking.seat_service.service.SeatHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shows")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    @PostMapping("/{showId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse hold(@PathVariable UUID showId,
                             @RequestBody @Valid HoldRequest request) {
        UUID holdId = seatHoldService.holdSeats(showId, request.seatIds());
        return new HoldResponse(holdId);
    }

    @PostMapping("/{showId}/holds/{holdId}/extend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void extendHold(@PathVariable UUID showId, @PathVariable UUID holdId, @RequestBody @Valid HoldRequest request) {
        seatHoldService.extendHold(showId, request.seatIds(), holdId);
    }
}
