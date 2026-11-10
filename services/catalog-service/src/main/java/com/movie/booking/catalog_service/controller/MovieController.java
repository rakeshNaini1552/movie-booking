package com.movie.booking.catalog_service.controller;

import com.movie.booking.catalog_service.MovieStatus;
import com.movie.booking.catalog_service.dto.MovieRequest;
import com.movie.booking.catalog_service.dto.MovieResponse;
import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.service.MovieService;
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

import java.util.UUID;

@Tag(name = "Movies", description = "Movie catalog management")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/movies")
public class MovieController {

    private final MovieService movieService;

    @Operation(summary = "Create a movie")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed body", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse create(@RequestBody @Valid MovieRequest request) {
        return movieService.create(request);
    }

    @Operation(summary = "Get a movie by id")
    @ApiResponse(responseCode = "404", description = "Movie not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public MovieResponse get(@PathVariable UUID id) {
        return movieService.get(id);
    }

    @Operation(summary = "List movies",
            description = "Optionally filtered by status and title; paged and sortable.")
    @ApiResponse(responseCode = "400", description = "Invalid filter value or unknown sort property", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public PageResponse<MovieResponse> list(@RequestParam(required = false) MovieStatus status,
                                            @RequestParam(required = false) String title,
                                            Pageable pageable) {
        return movieService.list(status, title, pageable);
    }

    @Operation(summary = "Update a movie")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed body", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Movie not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    public MovieResponse update(@PathVariable UUID id, @RequestBody @Valid MovieRequest request) {
        return movieService.update(id, request);
    }

    @Operation(summary = "Archive a movie",
            description = "Soft delete: the movie stays in the database with status ARCHIVED.")
    @ApiResponse(responseCode = "404", description = "Movie not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Movie has shows and cannot be deleted", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        movieService.archive(id);
    }
}
