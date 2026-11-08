package com.movie.booking.catalog_service.controller;

import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.dto.TheaterRequest;
import com.movie.booking.catalog_service.dto.TheaterResponse;
import com.movie.booking.catalog_service.service.TheaterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/theaters")
public class TheaterController {

    private final TheaterService theaterService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TheaterResponse create(@RequestBody @Valid TheaterRequest request) {
        return theaterService.create(request);
    }

    @GetMapping("/{id}")
    public TheaterResponse get(@PathVariable UUID id) {
        return theaterService.get(id);
    }

    @GetMapping
    public PageResponse<TheaterResponse> list(@RequestParam(required = false) String city, Pageable pageable) {
        return theaterService.list(city, pageable);
    }

    @PutMapping("/{id}")
    public TheaterResponse update(@PathVariable UUID id, @RequestBody @Valid TheaterRequest request) {
        return theaterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        theaterService.delete(id);
    }
}
