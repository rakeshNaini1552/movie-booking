package com.movie.booking.catalog_service.controller;

import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.dto.ShowRequest;
import com.movie.booking.catalog_service.dto.ShowResponse;
import com.movie.booking.catalog_service.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shows")
public class ShowController {

    private final ShowService showService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse create(@RequestBody @Valid ShowRequest request) {
        return showService.create(request);
    }

    @GetMapping("/{id}")
    public ShowResponse get(@PathVariable UUID id) {
        return showService.get(id);
    }

    // from is inclusive and to is exclusive, both compared with the show's start time.
    @GetMapping
    public PageResponse<ShowResponse> list(@RequestParam(required = false) UUID movieId,
                                           @RequestParam(required = false) UUID theaterId,
                                           @RequestParam(required = false) Instant from,
                                           @RequestParam(required = false) Instant to,
                                           Pageable pageable) {
        return showService.list(movieId, theaterId, from, to, pageable);
    }

    @PutMapping("/{id}")
    public ShowResponse update(@PathVariable UUID id, @RequestBody @Valid ShowRequest request) {
        return showService.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    public ShowResponse cancel(@PathVariable UUID id) {
        return showService.cancel(id);
    }
}
