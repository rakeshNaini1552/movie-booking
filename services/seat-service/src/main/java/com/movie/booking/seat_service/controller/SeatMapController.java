package com.movie.booking.seat_service.controller;


import com.movie.booking.seat_service.dto.SeatResponse;
import com.movie.booking.seat_service.service.SeatMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Seat map", description = "Seat availability per show")
@RestController
@RequestMapping("/api/v1/shows/{showId}/seats")
public class SeatMapController {
    // inject SeatMapService via constructor
    private final SeatMapService seatMapService;

    public SeatMapController(SeatMapService service){
        this.seatMapService = service;
    }

    @Operation(summary = "Get the seat map of a show",
            description = "Open to any authenticated user. Seats with a live hold are reported as HELD.")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Show has no seats", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public List<SeatResponse> getSeatMap(@PathVariable UUID showId) {
        return seatMapService.getSeatMap(showId);
    }
}
