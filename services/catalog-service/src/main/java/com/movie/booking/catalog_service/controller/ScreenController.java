package com.movie.booking.catalog_service.controller;

import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.dto.ScreenRequest;
import com.movie.booking.catalog_service.dto.ScreenResponse;
import com.movie.booking.catalog_service.service.ScreenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/theaters/{theaterId}/screens")
public class ScreenController {

    private final ScreenService screenService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScreenResponse create(@PathVariable UUID theaterId, @RequestBody @Valid ScreenRequest request) {
        return screenService.create(theaterId, request);
    }

    @GetMapping
    public PageResponse<ScreenResponse> list(@PathVariable UUID theaterId, Pageable pageable) {
        return screenService.list(theaterId, pageable);
    }

    @GetMapping("/{screenId}")
    public ScreenResponse get(@PathVariable UUID theaterId, @PathVariable UUID screenId) {
        return screenService.get(theaterId, screenId);
    }

    @PutMapping("/{screenId}")
    public ScreenResponse update(@PathVariable UUID theaterId, @PathVariable UUID screenId,
                                 @RequestBody @Valid ScreenRequest request) {
        return screenService.update(theaterId, screenId, request);
    }

    @DeleteMapping("/{screenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID theaterId, @PathVariable UUID screenId) {
        screenService.delete(theaterId, screenId);
    }
}
