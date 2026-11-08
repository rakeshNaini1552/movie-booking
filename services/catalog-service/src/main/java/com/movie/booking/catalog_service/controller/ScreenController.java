package com.movie.booking.catalog_service.controller;

import com.movie.booking.catalog_service.dto.PageResponse;
import com.movie.booking.catalog_service.dto.ScreenRequest;
import com.movie.booking.catalog_service.dto.ScreenResponse;
import com.movie.booking.catalog_service.service.ScreenService;
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

@Tag(name = "Screens", description = "Screen management within a theater")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/theaters/{theaterId}/screens")
public class ScreenController {

    private final ScreenService screenService;

    @Operation(summary = "Create a screen in a theater")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed body", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Theater not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A screen with this name already exists in the theater", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScreenResponse create(@PathVariable UUID theaterId, @RequestBody @Valid ScreenRequest request) {
        return screenService.create(theaterId, request);
    }

    @Operation(summary = "List the screens of a theater")
    @ApiResponse(responseCode = "400", description = "Unknown sort property", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Theater not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public PageResponse<ScreenResponse> list(@PathVariable UUID theaterId, Pageable pageable) {
        return screenService.list(theaterId, pageable);
    }

    @Operation(summary = "Get a screen by id")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Screen not found under this theater", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{screenId}")
    public ScreenResponse get(@PathVariable UUID theaterId, @PathVariable UUID screenId) {
        return screenService.get(theaterId, screenId);
    }

    @Operation(summary = "Update a screen")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed body", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Screen not found under this theater", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A screen with this name already exists in the theater", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{screenId}")
    public ScreenResponse update(@PathVariable UUID theaterId, @PathVariable UUID screenId,
                                 @RequestBody @Valid ScreenRequest request) {
        return screenService.update(theaterId, screenId, request);
    }

    @Operation(summary = "Delete a screen")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    @ApiResponse(responseCode = "404", description = "Screen not found under this theater", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Screen has shows and cannot be deleted", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{screenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID theaterId, @PathVariable UUID screenId) {
        screenService.delete(theaterId, screenId);
    }
}
