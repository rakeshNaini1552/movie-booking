package com.movie.booking.catalog_service.controller;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.dto.MovieRequest;
import com.movie.booking.catalog_service.dto.MovieResponse;
import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/movies")
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse create(@RequestBody @Valid MovieRequest request) {
        return movieService.create(request);
    }

    @GetMapping("/{id}")
    public MovieResponse get(@PathVariable UUID id) {
        return movieService.get(id);
    }

    @GetMapping
    public PageResponse<MovieResponse> list(@RequestParam(required = false) MovieStatus status,
                                            @RequestParam(required = false) String title,
                                            Pageable pageable) {
        return movieService.list(status, title, pageable);
    }

    @PutMapping("/{id}")
    public MovieResponse update(@PathVariable UUID id, @RequestBody @Valid MovieRequest request) {
        return movieService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        movieService.archive(id);
    }
}
