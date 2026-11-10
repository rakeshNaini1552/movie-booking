package com.movie.booking.catalog_service.controller;

import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.dto.ShowRequest;
import com.movie.booking.catalog_service.dto.ShowResponse;
import com.movie.booking.catalog_service.service.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Shows", description = "Show scheduling")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shows")
public class ShowController {

    private final ShowService showService;

    @Operation(summary = "Schedule a show")
    @ApiResponse(responseCode = "400", description = "Validation failed, malformed body, or endsAt not after startsAt", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Movie or screen not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Movie is archived or the screen already has an overlapping show", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse create(@RequestBody @Valid ShowRequest request) {
        return showService.create(request);
    }

    @Operation(summary = "Get a show by id")
    @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public ShowResponse get(@PathVariable UUID id) {
        return showService.get(id);
    }

    // from is inclusive and to is exclusive, both compared with the show's start time.
    @Operation(summary = "List shows",
            description = "Optionally filtered by movie, theater and a start-time window; paged and sortable.")
    @ApiResponse(responseCode = "400", description = "Invalid filter value or unknown sort property", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public PageResponse<ShowResponse> list(@RequestParam(required = false) UUID movieId,
                                           @RequestParam(required = false) UUID theaterId,
                                           @RequestParam(required = false) Instant from,
                                           @RequestParam(required = false) Instant to,
                                           Pageable pageable) {
        return showService.list(movieId, theaterId, from, to, pageable);
    }

    @Operation(summary = "Update a show")
    @ApiResponse(responseCode = "400", description = "Validation failed, malformed body, or endsAt not after startsAt", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Show, movie or screen not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Show is cancelled, movie is archived, or the screen already has an overlapping show", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    public ShowResponse update(@PathVariable UUID id, @RequestBody @Valid ShowRequest request) {
        return showService.update(id, request);
    }

    @Operation(summary = "Cancel a show",
            description = "Cancelling an already cancelled show returns its current state.")
    @ApiResponse(responseCode = "404", description = "Show not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/cancel")
    public ShowResponse cancel(@PathVariable UUID id) {
        return showService.cancel(id);
    }
}
