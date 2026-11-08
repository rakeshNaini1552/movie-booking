package com.movie.booking.catalog_service.controller;

import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.dto.TheaterRequest;
import com.movie.booking.catalog_service.dto.TheaterResponse;
import com.movie.booking.catalog_service.service.TheaterService;
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

@Tag(name = "Theaters", description = "Theater management")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/theaters")
public class TheaterController {

    private final TheaterService theaterService;

    @Operation(summary = "Create a theater")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed body", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TheaterResponse create(@RequestBody @Valid TheaterRequest request) {
        return theaterService.create(request);
    }

    @Operation(summary = "Get a theater by id")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Theater not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public TheaterResponse get(@PathVariable UUID id) {
        return theaterService.get(id);
    }

    @Operation(summary = "List theaters",
            description = "Optionally filtered by city; paged and sortable.")
    @ApiResponse(responseCode = "400", description = "Unknown sort property", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @GetMapping
    public PageResponse<TheaterResponse> list(@RequestParam(required = false) String city, Pageable pageable) {
        return theaterService.list(city, pageable);
    }

    @Operation(summary = "Update a theater")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed body", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Theater not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    public TheaterResponse update(@PathVariable UUID id, @RequestBody @Valid TheaterRequest request) {
        return theaterService.update(id, request);
    }

    @Operation(summary = "Delete a theater",
            description = "Hard delete: its screens are deleted with it.")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Theater not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Theater has shows and cannot be deleted", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        theaterService.delete(id);
    }
}
