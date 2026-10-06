package com.movie.booking.seat_service.controller;


import com.movie.booking.seat_service.SeatResponse;
import com.movie.booking.seat_service.service.SeatMapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shows/{showId}/seats")
public class SeatMapController {
    // inject SeatMapService via constructor
    private final SeatMapService seatMapService;

    public SeatMapController(SeatMapService service){
        this.seatMapService = service;
    }

    @GetMapping
    public List<SeatResponse> getSeatMap(@PathVariable UUID showId) {
        return seatMapService.getSeatMap(showId);
    }
}
