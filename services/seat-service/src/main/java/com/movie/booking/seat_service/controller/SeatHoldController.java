package com.movie.booking.seat_service.controller;

import com.movie.booking.seat_service.dto.ExtendRequest;
import com.movie.booking.seat_service.dto.HoldRequest;
import com.movie.booking.seat_service.dto.ReleaseRequest;
import com.movie.booking.seat_service.service.SeatHoldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Seat holds", description = "Temporary seat holds (internal-service role)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shows")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    @Operation(summary = "Hold seats for a booking",
            description = "Requires the internal-service role. Holds the seats in Redis for the configured TTL.")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed body")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "403", description = "Token lacks the internal-service role")
    @ApiResponse(responseCode = "409", description = "One or more seats are not available", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{showId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public void hold(@PathVariable UUID showId,
                     @RequestBody @Valid HoldRequest request) {
        seatHoldService.holdSeats(showId, request.seatIds(), request.bookingId());
    }

    @Operation(summary = "Extend a hold",
            description = "Requires the internal-service role. Resets the hold TTL.")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed body")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "403", description = "Token lacks the internal-service role")
    @ApiResponse(responseCode = "409", description = "The hold is gone or belongs to another booking", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{showId}/holds/{bookingId}/extend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void extendHold(@PathVariable UUID showId,
                           @PathVariable UUID bookingId,
                           @RequestBody @Valid ExtendRequest request) {
        seatHoldService.extendHold(showId, request.seatIds(), bookingId);
    }

    @Operation(summary = "Release a hold",
            description = "Requires the internal-service role. Releasing seats that are not held is not an error.")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed body")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "403", description = "Token lacks the internal-service role")
    @PostMapping("/{showId}/holds/{bookingId}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseHold(@PathVariable UUID showId,
                            @PathVariable UUID bookingId,
                            @RequestBody @Valid ReleaseRequest request) {
        seatHoldService.releaseHold(showId, request.seatIds(), bookingId);
    }
}
