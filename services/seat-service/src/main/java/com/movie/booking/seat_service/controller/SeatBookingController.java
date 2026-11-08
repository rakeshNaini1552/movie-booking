package com.movie.booking.seat_service.controller;

import com.movie.booking.seat_service.dto.BookingResponse;
import com.movie.booking.seat_service.dto.ConfirmRequest;
import com.movie.booking.seat_service.service.SeatConfirmService;
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

@Tag(name = "Seat confirmation", description = "Permanent seat booking (internal-service role)")
@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
public class SeatBookingController {

    private final SeatConfirmService seatConfirmService;

    @Operation(summary = "Confirm held seats for a booking",
            description = "Requires the internal-service role. Marks the seats BOOKED in the database.")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed body")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "403", description = "Token lacks the internal-service role")
    @ApiResponse(responseCode = "409", description = "Seats are not held by this booking, or were just booked concurrently", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{showId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse confirm(@PathVariable UUID showId,
                                   @RequestBody @Valid ConfirmRequest request) {
        seatConfirmService.confirm(showId, request.seatIds(), request.bookingId());
        return new BookingResponse(request.bookingId());
    }
}
